package etl;

import etl.aggregators.BatchAggregator;
import etl.mappers.CurrencyRateLookup;
import etl.mappers.CustomerLookup;
import etl.mappers.ProductLookup;
import etl.mappers.PurchaseOrderToSalesOrder;
import etl.mappers.SalesOrderToInventoryAdjustment;
import etl.mappers.SalesOrderToInvoice;
import etl.mappers.SalesOrderToShipment;
import etl.model.BatchSummary;
import etl.model.Customer;
import etl.model.InventoryAdjustment;
import etl.model.Invoice;
import etl.model.PurchaseOrder;
import etl.model.SalesOrder;
import etl.model.Shipment;
import etl.model.ValidationIssue;
import etl.pipeline.AuditLog;
import etl.pipeline.RunRecord;
import etl.pipeline.Router;
import etl.readers.CsvPurchaseOrderReader;
import etl.readers.JsonPurchaseOrderReader;
import etl.readers.SegmentedPurchaseOrderReader;
import etl.readers.XmlPurchaseOrderReader;
import etl.validators.OrderValidator;
import etl.writers.AggregateReportWriter;
import etl.writers.CsvReportWriter;
import etl.writers.InventoryAdjustmentXmlWriter;
import etl.writers.InvoiceXmlWriter;
import etl.writers.SalesOrderJsonWriter;
import etl.writers.SalesOrderXmlWriter;
import etl.writers.ShipmentXmlWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
 *   inputs (XML / CSV / JSON / segmented)
 *      │
 *      ▼
 *   ┌──────────┐        ┌────────────────────────────┐
 *   │  read    │  PO →  │  OrderValidator            │
 *   └──────────┘        └────────────┬───────────────┘
 *                                    │            ┌── errors ──► output/quarantine/
 *                                    ▼ ok/warn
 *                       ┌────────────────────────────┐
 *                       │  PurchaseOrderToSalesOrder │  + Customer + Currency lookups (FX)
 *                       └────────────┬───────────────┘
 *                                    ▼  SalesOrder
 *                       ┌──── SalesOrderToInvoice ────┐
 *                       │     SalesOrderToShipment    │
 *                       │     SalesOrderToInventory   │
 *                       └────────────┬───────────────┘
 *                                    ▼
 *                       Router → high_value | standard
 *                       Writers → SalesOrder XML+JSON, Invoice XML,
 *                                 Shipment XML, Inventory XML
 *                                    │
 *                                    ▼
 *                       BatchAggregator (customer / region / currency / carrier)
 *                                    │
 *                                    ▼
 *                       AuditLog + summary CSV + aggregates CSV
 * </pre>
 */
public final class EtlPipeline {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");

    private static final Path PROJECT_ROOT      = Paths.get("").toAbsolutePath();
    private static final Path INPUT_DIR         = PROJECT_ROOT.resolve("data/input");
    private static final Path REFERENCE_DIR     = PROJECT_ROOT.resolve("data/reference");
    private static final Path CUSTOMER_MASTER   = REFERENCE_DIR.resolve("customer_master.csv");
    private static final Path PRODUCT_MASTER    = REFERENCE_DIR.resolve("product_master.csv");
    private static final Path CURRENCY_RATES    = REFERENCE_DIR.resolve("currency_rates.csv");

    private static final Path OUTPUT_DIR        = PROJECT_ROOT.resolve("output");
    private static final Path SALES_ORDERS_DIR  = OUTPUT_DIR.resolve("sales_orders");
    private static final Path HIGH_VALUE_DIR    = SALES_ORDERS_DIR.resolve("high_value");
    private static final Path STANDARD_DIR      = SALES_ORDERS_DIR.resolve("standard");
    private static final Path SO_JSON_DIR       = OUTPUT_DIR.resolve("sales_orders_json");
    private static final Path INVOICES_DIR      = OUTPUT_DIR.resolve("invoices");
    private static final Path SHIPMENTS_DIR     = OUTPUT_DIR.resolve("shipments");
    private static final Path INVENTORY_DIR     = OUTPUT_DIR.resolve("inventory_adjustments");
    private static final Path QUARANTINE_DIR    = OUTPUT_DIR.resolve("quarantine");
    private static final Path AGGREGATES_DIR    = OUTPUT_DIR.resolve("aggregates");
    private static final Path AUDIT_LOG_FILE    = OUTPUT_DIR.resolve("audit.log");
    private static final Path SUMMARY_REPORT    = OUTPUT_DIR.resolve("summary.csv");

    private final List<RunRecord>  records      = new ArrayList<>();
    private final List<SalesOrder> salesOrders  = new ArrayList<>();

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

        // ---- 0. Reference data --------------------------------------------------
        log("Loading reference data");
        CustomerLookup     customers  = CustomerLookup.load(CUSTOMER_MASTER);
        ProductLookup      products   = ProductLookup.load(PRODUCT_MASTER);
        CurrencyRateLookup currencies = CurrencyRateLookup.load(CURRENCY_RATES);
        OrderValidator     validator  = new OrderValidator(products, currencies);
        log("  customers=" + customers.size()
                + "  products=" + products.size()
                + "  currencies=" + currencies.size());

        Router router = new Router(HIGH_VALUE_THRESHOLD, HIGH_VALUE_DIR, STANDARD_DIR);

        // ---- 1. Extract ---------------------------------------------------------
        log("Stage 1/5: Extracting input documents from " + relative(INPUT_DIR));
        List<Path> xmlFiles = listFiles(INPUT_DIR, "*.xml");
        List<Path> csvFiles = listFiles(INPUT_DIR, "*.csv");
        List<Path> jsonFiles = listFiles(INPUT_DIR, "*.json");
        List<Path> segFiles = listFiles(INPUT_DIR, "*.fwf");
        log(String.format("  XML=%d  CSV=%d  JSON=%d  Segmented=%d",
                xmlFiles.size(), csvFiles.size(), jsonFiles.size(), segFiles.size()));

        // ---- 2. Read & process --------------------------------------------------
        log("Stage 2/5: Validate + map + write outputs");

        for (Path xml : xmlFiles) {
            String src = xml.getFileName().toString();
            try {
                PurchaseOrder po = XmlPurchaseOrderReader.read(xml);
                processOne(po, xml, src, validator, customers, currencies, products, router);
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
                    processOne(po, csv, src + "#" + po.orderId(),
                            validator, customers, currencies, products, router);
                }
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "CSV", e));
            }
        }
        for (Path json : jsonFiles) {
            String src = json.getFileName().toString();
            try {
                List<PurchaseOrder> orders = JsonPurchaseOrderReader.read(json);
                if (orders.isEmpty()) {
                    records.add(RunRecord.failure(src, "JSON",
                            new IllegalStateException("Empty array")));
                    continue;
                }
                for (PurchaseOrder po : orders) {
                    processOne(po, json, src + "#" + po.orderId(),
                            validator, customers, currencies, products, router);
                }
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "JSON", e));
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
                    processOne(po, seg, src + "#" + po.orderId(),
                            validator, customers, currencies, products, router);
                }
            } catch (Exception e) {
                records.add(RunRecord.failure(src, "Segmented", e));
            }
        }

        // ---- 3. Aggregate -------------------------------------------------------
        log("Stage 3/5: Aggregating rollups (customer / region / currency / carrier)");
        List<BatchSummary> summaries = List.of(
                BatchAggregator.byCustomer(records, salesOrders),
                BatchAggregator.byRegion(records, salesOrders),
                BatchAggregator.byCurrency(records, salesOrders),
                BatchAggregator.byCarrier(records, salesOrders));
        AggregateReportWriter.writeAll(summaries, AGGREGATES_DIR);

        // ---- 4. Audit + summary -------------------------------------------------
        log("Stage 4/5: Writing audit log + summary report");
        AuditLog.write(records, AUDIT_LOG_FILE, batchId, processedAt, HIGH_VALUE_THRESHOLD);
        CsvReportWriter.write(records, PROJECT_ROOT, SUMMARY_REPORT);

        // ---- 5. Console summary -------------------------------------------------
        log("Stage 5/5: Printing run summary");
        printRunSummary(summaries);
    }

    /** End-to-end processing for a single {@link PurchaseOrder}. */
    private void processOne(PurchaseOrder po,
                            Path sourceFile,
                            String sourceName,
                            OrderValidator validator,
                            CustomerLookup customers,
                            CurrencyRateLookup currencies,
                            ProductLookup products,
                            Router router) {
        try {
            // Stage 2a: validate the source PO before any mapping
            List<ValidationIssue> issues = validator.validate(po);
            if (OrderValidator.hasErrors(issues)) {
                Path quarantineFile = QUARANTINE_DIR.resolve(po.orderId() + "__"
                        + sanitiseFileName(sourceFile.getFileName().toString()));
                Files.copy(sourceFile, quarantineFile, StandardCopyOption.REPLACE_EXISTING);
                writeIssueDescriptor(quarantineFile, po, issues);
                records.add(RunRecord.quarantined(sourceName, po.orderId(),
                        po.customer(), quarantineFile, issues));
                return;
            }

            // Stage 2b: PO -> SalesOrder (with customer + FX enrichment)
            SalesOrder so = PurchaseOrderToSalesOrder.map(
                    po, customers, currencies, batchId, processedAt);
            Customer   c  = customers.find(po.customer());
            salesOrders.add(so);

            // Stage 2c: SalesOrder -> Invoice
            Invoice inv = SalesOrderToInvoice.map(so, c, today);

            // Stage 2d: SalesOrder -> Shipment
            Shipment ship = SalesOrderToShipment.map(so);

            // Stage 2e: SalesOrder -> InventoryAdjustment
            InventoryAdjustment adj = SalesOrderToInventoryAdjustment.map(so, products);

            // Stage 2f: route + write 5 artifacts (XML + JSON + invoice + shipment + inventory)
            Path soXmlOut    = router.destinationFor(so).resolve(so.id() + ".xml");
            Path soJsonOut   = SO_JSON_DIR.resolve(so.id() + ".json");
            Path invoiceOut  = INVOICES_DIR.resolve(inv.invoiceNumber() + ".xml");
            Path shipmentOut = SHIPMENTS_DIR.resolve(ship.shipmentNumber() + ".xml");
            Path adjOut      = INVENTORY_DIR.resolve(adj.adjustmentNumber() + ".xml");

            SalesOrderXmlWriter.write(so, soXmlOut);
            SalesOrderJsonWriter.write(so, soJsonOut);
            InvoiceXmlWriter.write(inv, invoiceOut);
            ShipmentXmlWriter.write(ship, shipmentOut);
            InventoryAdjustmentXmlWriter.write(adj, adjOut);

            records.add(RunRecord.accepted(
                    sourceName, so.id(), so.customerName(), so.customerCode(), so.currency(),
                    so.itemCount(), so.grandTotal(), so.grandTotalUsd(),
                    router.isHighValue(so),
                    soXmlOut, soJsonOut, invoiceOut, shipmentOut, adjOut,
                    ship.carrier(),
                    issues));
        } catch (Exception e) {
            records.add(RunRecord.failure(sourceName, "Processing", e));
        }
    }

    private void writeIssueDescriptor(Path quarantineFile,
                                      PurchaseOrder po,
                                      List<ValidationIssue> issues) throws IOException {
        Path txt = quarantineFile.resolveSibling(quarantineFile.getFileName() + ".issues.txt");
        StringBuilder sb = new StringBuilder();
        sb.append("# Validation issues for ").append(po.orderId()).append('\n');
        sb.append("# Source: ").append(quarantineFile.getFileName()).append('\n');
        sb.append("# Customer: ").append(po.customer()).append('\n');
        sb.append('\n');
        for (ValidationIssue issue : issues) {
            sb.append(String.format("%-7s  %s  %s%n",
                    issue.severity(), issue.code(), issue.message()));
        }
        Files.writeString(txt, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String sanitiseFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._\\-#]", "_");
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
        Files.createDirectories(SO_JSON_DIR);
        Files.createDirectories(INVOICES_DIR);
        Files.createDirectories(SHIPMENTS_DIR);
        Files.createDirectories(INVENTORY_DIR);
        Files.createDirectories(QUARANTINE_DIR);
        Files.createDirectories(AGGREGATES_DIR);
    }

    private static List<Path> listFiles(Path dir, String glob) throws IOException {
        if (!Files.exists(dir)) return List.of();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private void printRunSummary(List<BatchSummary> summaries) {
        long total       = records.size();
        long accepted    = records.stream().filter(RunRecord::isAccepted).count();
        long quarantined = records.stream().filter(RunRecord::isQuarantined).count();
        long failed      = records.stream().filter(RunRecord::isFailure).count();
        long highVal     = records.stream().filter(r -> r.isAccepted() && r.isHighValue()).count();
        long standard    = accepted - highVal;

        BigDecimal totalUsd = records.stream()
                .filter(RunRecord::isAccepted)
                .map(RunRecord::grandTotalUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        banner("ETL Pipeline finished");
        System.out.println("  Processed:     " + total);
        System.out.println("  Accepted:      " + accepted);
        System.out.println("  Quarantined:   " + quarantined + "  -> " + relative(QUARANTINE_DIR));
        System.out.println("  Failed:        " + failed);
        System.out.println("  High value:    " + highVal + "  -> " + relative(HIGH_VALUE_DIR));
        System.out.println("  Standard:      " + standard + "  -> " + relative(STANDARD_DIR));
        System.out.println("  SO JSON:       " + accepted + "  -> " + relative(SO_JSON_DIR));
        System.out.println("  Invoices:      " + accepted + "  -> " + relative(INVOICES_DIR));
        System.out.println("  Shipments:     " + accepted + "  -> " + relative(SHIPMENTS_DIR));
        System.out.println("  Inventory:     " + accepted + "  -> " + relative(INVENTORY_DIR));
        System.out.println("  Total USD:     " + totalUsd.toPlainString());
        System.out.println("  Audit log:     " + relative(AUDIT_LOG_FILE));
        System.out.println("  Summary CSV:   " + relative(SUMMARY_REPORT));
        System.out.println("  Aggregates:    " + relative(AGGREGATES_DIR) + "/by_*.csv");
        System.out.println();

        for (RunRecord r : records) {
            String tag;
            switch (r.status()) {
                case ACCEPTED   -> tag = "OK  ";
                case QUARANTINED-> tag = "QUAR";
                default         -> tag = "FAIL";
            }
            String tail;
            if (r.isAccepted()) {
                tail = String.format("%-10s -> %-10s  %s %12s (USD %s) carrier=%s",
                        r.orderId(), r.routedTo(),
                        r.currency(), r.grandTotal().toPlainString(),
                        r.grandTotalUsd().toPlainString(),
                        r.carrier());
            } else if (r.isQuarantined()) {
                tail = String.format("%-10s -> QUARANTINE  %d issue(s)",
                        r.orderId(), r.issues().size());
            } else {
                tail = r.error();
            }
            System.out.printf("  [%s] %-32s  %s%n", tag, r.source(), tail);
        }

        System.out.println();
        for (BatchSummary s : summaries) {
            System.out.printf("  Aggregate by %-9s : %d bucket(s) -> by_%s.csv%n",
                    s.dimension(), s.buckets().size(), s.dimension().toLowerCase());
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
