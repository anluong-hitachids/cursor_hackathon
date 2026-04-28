package etl.writers;

import etl.pipeline.RunRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a per-run summary CSV with one row per processed record. */
public final class CsvReportWriter {

    private CsvReportWriter() {}

    public static void write(List<RunRecord> records, Path projectRoot, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write(String.join(",",
                    "Source","OrderId","Customer","CustomerCode","Currency",
                    "ItemCount","GrandTotal","Route","SalesOrderFile","InvoiceFile",
                    "ShipmentFile","Carrier","Error"));
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
                        csv(r.routedTo()),
                        relativeOrEmpty(projectRoot, r.salesOrderFile()),
                        relativeOrEmpty(projectRoot, r.invoiceFile()),
                        relativeOrEmpty(projectRoot, r.shipmentFile()),
                        csv(r.carrier() == null ? "" : r.carrier()),
                        csv(r.error() == null ? "" : r.error())));
                w.newLine();
            }
        }
    }

    private static String relativeOrEmpty(Path projectRoot, Path p) {
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
