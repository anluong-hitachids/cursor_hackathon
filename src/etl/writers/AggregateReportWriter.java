package etl.writers;

import etl.model.BatchSummary;
import etl.model.BatchSummary.Bucket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes one CSV per dimension produced by {@code BatchAggregator}.
 * One row per bucket, with order/item counts and value rollups.
 */
public final class AggregateReportWriter {

    private AggregateReportWriter() {}

    public static void writeAll(List<BatchSummary> summaries, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        for (BatchSummary summary : summaries) {
            String fileName = "by_" + summary.dimension().toLowerCase() + ".csv";
            write(summary, targetDir.resolve(fileName));
        }
    }

    public static void write(BatchSummary summary, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write(summary.dimension() + ",OrderCount,ItemCount,TotalGrandTotal,TotalUSD");
            w.newLine();
            for (Bucket b : summary.buckets()) {
                w.write(String.join(",",
                        csv(b.key()),
                        Integer.toString(b.orderCount()),
                        Integer.toString(b.itemCount()),
                        b.totalGrandTotal().toPlainString(),
                        b.totalUsd().toPlainString()));
                w.newLine();
            }
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
