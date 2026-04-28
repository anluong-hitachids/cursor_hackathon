package etl.pipeline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a human-readable audit log summarising each {@link RunRecord}. */
public final class AuditLog {

    private AuditLog() {}

    public static void write(List<RunRecord> records,
                             Path target,
                             String batchId,
                             String processedAt,
                             BigDecimal threshold) throws IOException {
        Files.createDirectories(target.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write("# ETL run " + batchId);                      w.newLine();
            w.write("# processedAt=" + processedAt);              w.newLine();
            w.write("# highValueThreshold=" + threshold);         w.newLine();
            w.newLine();
            for (RunRecord r : records) {
                if (r.error() == null) {
                    w.write(String.format(
                            "OK   %-32s %-10s -> %-10s  %3d items  %s %s  carrier=%s",
                            r.source(), r.orderId(), r.routedTo(),
                            r.itemCount(), r.currency(),
                            r.grandTotal().toPlainString(),
                            r.carrier()));
                } else {
                    w.write(String.format("FAIL %-32s %s", r.source(), r.error()));
                }
                w.newLine();
            }
        }
    }
}
