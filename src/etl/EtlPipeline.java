package etl;

import etl.mappers.CustomerLookup;
import etl.mappers.PurchaseOrderToSalesOrder;
import etl.mappers.SalesOrderToInvoice;
import etl.mappers.SalesOrderToShipment;
import etl.model.Customer;
import etl.model.Invoice;
import etl.model.PurchaseOrder;
import etl.model.SalesOrder;
import etl.model.Shipment;
import etl.pipeline.AuditLog;
import etl.pipeline.RunRecord;
import etl.pipeline.Router;
import etl.readers.CsvPurchaseOrderReader;
import etl.readers.SegmentedPurchaseOrderReader;
import etl.readers.XmlPurchaseOrderReader;
import etl.writers.CsvReportWriter;
import etl.writers.InvoiceXmlWriter;
import etl.writers.SalesOrderXmlWriter;
import etl.writers.ShipmentXmlWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Multi-stage ETL pipeline orchestrator.
 *
 * <pre>
 *   data/input/*.xml     ┐
 *   data/input/*.csv     ┤  read   PurchaseOrder      Map (PO->SO,         Route + write
 *   data/input/*.fwf|txt ┘  ───►   POJOs              SO->Invoice,        ───► output/sales_orders/
 *                                                     SO->Shipment)            output/invoices/
 *                          customer_master.csv ───────────────────────►        output/shipments/
 *                                                                              + audit.log + summary.csv
 * </pre>
 */
public final class EtlPipeline {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");

    private static final Path PROJECT_ROOT      = Paths.get("").toAbsolutePath();
    private static final Path INPUT_DIR         = PROJECT_ROOT.resolve("data/input");
    private static final Path REFERENCE_DIR     = PROJECT_ROOT.resolve("data/reference");
    private static final Path CUSTOMER_MASTER   = REFERENCE_DIR.resolve("customer_master.csv");

    private static final Path OUTPUT_DIR        = PROJECT_ROOT.resolve("output");
    private static final Path SALES_ORDERS_DIR  = OUTPUT_DIR.resolve("sales_orders");
    private static final Path HIGH_VALUE_DIR    = SALES_ORDERS_DIR.resolve("high_value");
    private static final Path STANDARD_DIR      = SALES_ORDERS_DIR.resolve("standard");
    private static final Path INVOICES_DIR      = OUTPUT_DIR.resolve("invoices");
    private static final Path SHIPMENTS_DIR     = OUTPUT_DIR.resolve("shipments");
    private static final Path AUDIT_LOG_FILE    = OUTPUT_DIR.resolve("audit.log");
    private static final Path SUMMARY_REPORT    = OUTPUT_DIR.resolve("summary.csv");

    private final List<RunRecord> records = new ArrayList<>();
    private final String batchId =
            "B-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    private final String processedAt = Instant.now().toString();
    private final LocalDate today    = LocalDate.now();

    public static void main(String[] args) throws Exception {
        new EtlPipeline().run();
    }

    public void run() throws Exception {
        banner("ETL Pipeline starting   batchId=" + batchId);

        prepareOutputDirs();

        log("Loading reference data: " + relative(CUSTOMER_MASTER));
        CustomerLookup customers = CustomerLookup.load(CUSTOMER_MASTER);
        log("  Loaded " + customers.size() + " customer(s)");

        Router router = new Router(HIGH_VALUE_THRESHOLD, HIGH_VALUE_DIR, STANDARD_DIR);

        log("Stage 1/4: Extracting input documents from " + relative(INPUT_DIR));
        List<Path> xmlFiles = listFiles(INPUT_DIR, "*.xml");
        List<Path> csvFiles = listFiles(INPUT_DIR, "*.csv");
        List<Path> segFiles = listFiles(INPUT_DIR, "*.fwf");
        log(String.format("  XML=%d  CSV=%d  Segmented=%d",
                xmlFiles.size(), csvFiles.size(), segFiles.size()));

        log("Stage 2/4: Reading + mapping (PO -> SalesOrder, +Invoice, +Shipment)");

        for (Path xml : xmlFiles) {
            String src = xml.getFileName().toString();
            try {
                PurchaseOrder po = XmlPurchaseOrderReader.read(xml);
                processOne(po, src, customers, router);
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "XML", e));
            }
        }

        for (Path csv : csvFiles) {
            String src = csv.getFileName().toString();
            try {
                List<PurchaseOrder> grouped = CsvPurchaseOrderReader.read(csv);
                if (grouped.isEmpty()) {
                    records.add(RunRecord.failure(src, "CSV",
                            new IllegalStateException("No data rows")));
                    continue;
                }
                for (PurchaseOrder po : grouped) {
                    processOne(po, src + "#" + po.orderId(), customers, router);
                }
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "CSV", e));
            }
        }

        for (Path seg : segFiles) {
            String src = seg.getFileName().toString();
            try {
                List<PurchaseOrder> grouped = SegmentedPurchaseOrderReader.read(seg);
                if (grouped.isEmpty()) {
                    records.add(RunRecord.failure(src, "Segmented",
                            new IllegalStateException("No HDR segments")));
                    continue;
                }
                for (PurchaseOrder po : grouped) {
                    processOne(po, src + "#" + po.orderId(), customers, router);
                }
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "Segmented", e));
            }
        }

        log("Stage 3/4: Routing complete");
        log("Stage 4/4: Writing audit log + summary report");
        AuditLog.write(records, AUDIT_LOG_FILE, batchId, processedAt, HIGH_VALUE_THRESHOLD);
        CsvReportWriter.write(records, PROJECT_ROOT, SUMMARY_REPORT);

        printRunSummary();
    }

    /** End-to-end processing for a single {@link PurchaseOrder}. */
    private void processOne(PurchaseOrder po,
                            String sourceName,
                            CustomerLookup customers,
                            Router router) {
        try {
            // Stage 2a: PO -> SalesOrder (with customer enrichment)
            SalesOrder so = PurchaseOrderToSalesOrder.map(po, customers, batchId, processedAt);
            Customer   c  = customers.find(po.customer());

            // Stage 2b: SalesOrder -> Invoice
            Invoice inv = SalesOrderToInvoice.map(so, c, today);

            // Stage 2c: SalesOrder -> Shipment
            Shipment ship = SalesOrderToShipment.map(so);

            // Stage 3: route SalesOrder + write all three artifacts
            Path soOut   = router.destinationFor(so).resolve(so.id() + ".xml");
            Path invOut  = INVOICES_DIR.resolve(inv.invoiceNumber() + ".xml");
            Path shipOut = SHIPMENTS_DIR.resolve(ship.shipmentNumber() + ".xml");

            SalesOrderXmlWriter.write(so, soOut);
            InvoiceXmlWriter.write(inv, invOut);
            ShipmentXmlWriter.write(ship, shipOut);

            records.add(RunRecord.success(
                    sourceName, so.id(), so.customerName(), so.customerCode(), so.currency(),
                    so.itemCount(), so.grandTotal(), router.isHighValue(so),
                    soOut, invOut, shipOut, ship.carrier()));
        } catch (Exception e) {
            records.add(RunRecord.failure(sourceName, "Processing", e));
        }
    }

    // -------------------------------------------------------------------------
    // Setup + reporting
    // -------------------------------------------------------------------------

    private void prepareOutputDirs() throws IOException {
        if (Files.exists(OUTPUT_DIR)) {
            try (Stream<Path> walk = Files.walk(OUTPUT_DIR)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
            }
        }
        Files.createDirectories(HIGH_VALUE_DIR);
        Files.createDirectories(STANDARD_DIR);
        Files.createDirectories(INVOICES_DIR);
        Files.createDirectories(SHIPMENTS_DIR);
    }

    private static List<Path> listFiles(Path dir, String glob) throws IOException {
        if (!Files.exists(dir)) return List.of();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private void printRunSummary() {
        long ok       = records.stream().filter(r -> r.error() == null).count();
        long failed   = records.size() - ok;
        long highVal  = records.stream().filter(r -> r.error() == null && r.isHighValue()).count();
        long standard = ok - highVal;
        BigDecimal total = records.stream()
                .filter(r -> r.error() == null)
                .map(RunRecord::grandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        banner("ETL Pipeline finished");
        System.out.println("  Processed:    " + records.size());
        System.out.println("  Successful:   " + ok);
        System.out.println("  Failed:       " + failed);
        System.out.println("  High value:   " + highVal + "  -> " + relative(HIGH_VALUE_DIR));
        System.out.println("  Standard:     " + standard + "  -> " + relative(STANDARD_DIR));
        System.out.println("  Invoices:     " + ok + "  -> " + relative(INVOICES_DIR));
        System.out.println("  Shipments:    " + ok + "  -> " + relative(SHIPMENTS_DIR));
        System.out.println("  Total value:  " + total.toPlainString() + " (mixed currencies, demo only)");
        System.out.println("  Audit log:    " + relative(AUDIT_LOG_FILE));
        System.out.println("  Summary:      " + relative(SUMMARY_REPORT));
        System.out.println();

        for (RunRecord r : records) {
            String status = r.error() == null ? "OK  " : "FAIL";
            String tail   = r.error() == null
                    ? String.format("%-10s -> %-10s  %s %12s  carrier=%s",
                            r.orderId(), r.routedTo(),
                            r.currency(), r.grandTotal().toPlainString(),
                            r.carrier())
                    : r.error();
            System.out.printf("  [%s] %-32s  %s%n", status, r.source(), tail);
        }
    }

    private static Path relative(Path p) {
        return PROJECT_ROOT.relativize(p);
    }

    private static void log(String msg) {
        System.out.println("  " + msg);
    }

    private static void banner(String msg) {
        String bar = "=".repeat(Math.max(60, msg.length() + 4));
        System.out.println();
        System.out.println(bar);
        System.out.println("  " + msg);
        System.out.println(bar);
    }
}
