package etl.writers;

import etl.model.ValidationIssue;
import etl.pipeline.RunRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Writes a per-run summary CSV with one row per processed record. */
public final class CsvReportWriter {

    private CsvReportWriter() {}

    public static void write(List<RunRecord> records, Path projectRoot, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write(String.join(",",
                    "Source","OrderId","Customer","CustomerCode","Currency",
                    "ItemCount","GrandTotal","GrandTotalUsd","Status","Route",
                    "SalesOrderXml","SalesOrderJson","InvoiceFile","ShipmentFile",
                    "InventoryFile","QuarantineFile","Carrier","Issues","Error"));
            w.newLine();

            for (RunRecord r : records) {
                w.write(String.join(",",
                        csv(r.source()),
                        csv(r.orderId()),
                        csv(r.customer()),
                        csv(r.customerCode()),
                        csv(r.currency()),
                        Integer.toString(r.itemCount()),
                        r.grandTotal().toPlainString(),
                        r.grandTotalUsd().toPlainString(),
                        csv(r.status().name()),
                        csv(r.routedTo()),
                        rel(projectRoot, r.salesOrderXmlFile()),
                        rel(projectRoot, r.salesOrderJsonFile()),
                        rel(projectRoot, r.invoiceFile()),
                        rel(projectRoot, r.shipmentFile()),
                        rel(projectRoot, r.inventoryAdjustmentFile()),
                        rel(projectRoot, r.quarantineFile()),
                        csv(r.carrier() == null ? "" : r.carrier()),
                        csv(formatIssues(r.issues())),
                        csv(r.error() == null ? "" : r.error())));
                w.newLine();
            }
        }
    }

    private static String formatIssues(List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) return "";
        return issues.stream()
                .map(i -> i.severity() + ":" + i.code() + ":" + i.message())
                .collect(Collectors.joining(" | "));
    }

    private static String rel(Path projectRoot, Path p) {
        return csv(p == null ? "" : projectRoot.relativize(p).toString());
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
