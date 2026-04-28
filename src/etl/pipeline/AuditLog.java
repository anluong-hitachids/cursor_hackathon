package etl.pipeline;

import etl.model.ValidationIssue;

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
                switch (r.status()) {
                    case ACCEPTED ->
                        w.write(String.format(
                                "OK    %-32s %-10s -> %-10s  %3d items  %s %12s (USD %s) carrier=%s",
                                r.source(), r.orderId(), r.routedTo(),
                                r.itemCount(), r.currency(),
                                r.grandTotal().toPlainString(),
                                r.grandTotalUsd().toPlainString(),
                                r.carrier()));
                    case QUARANTINED ->
                        w.write(String.format(
                                "QUAR  %-32s %-10s -> QUARANTINE  %d issue(s)",
                                r.source(), r.orderId(),
                                r.issues() == null ? 0 : r.issues().size()));
                    case FAILED ->
                        w.write(String.format("FAIL  %-32s %s", r.source(), r.error()));
                }
                w.newLine();

                // Append validation issues (errors + warnings) under the parent line
                if (r.issues() != null) {
                    for (ValidationIssue issue : r.issues()) {
                        w.write(String.format("        %-7s %s  %s",
                                issue.severity(), issue.code(), issue.message()));
                        w.newLine();
                    }
                }
            }
        }
    }
}
