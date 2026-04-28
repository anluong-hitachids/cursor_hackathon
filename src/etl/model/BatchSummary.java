package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated rollups for one ETL run.  The pipeline computes one of these
 * per dimension (customer / region / carrier) and writes them to CSV.
 */
public record BatchSummary(String dimension, List<Bucket> buckets) {

    public record Bucket(String key,
                         int orderCount,
                         int itemCount,
                         BigDecimal totalGrandTotal,
                         BigDecimal totalUsd) {}
}
