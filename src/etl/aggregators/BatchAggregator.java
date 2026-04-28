package etl.aggregators;

import etl.model.BatchSummary;
import etl.model.BatchSummary.Bucket;
import etl.model.SalesOrder;
import etl.pipeline.RunRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds aggregated rollups across all successful records in a run,
 * grouped by various dimensions (customer, region, currency, carrier).
 */
public final class BatchAggregator {

    private BatchAggregator() {}

    public static BatchSummary byCustomer(List<RunRecord> records, List<SalesOrder> salesOrders) {
        return rollup("Customer", records, salesOrders, RunRecord::customer);
    }

    public static BatchSummary byCurrency(List<RunRecord> records, List<SalesOrder> salesOrders) {
        return rollup("Currency", records, salesOrders, RunRecord::currency);
    }

    public static BatchSummary byCarrier(List<RunRecord> records, List<SalesOrder> salesOrders) {
        return rollup("Carrier", records, salesOrders, r -> r.carrier() == null ? "" : r.carrier());
    }

    public static BatchSummary byRegion(List<RunRecord> records, List<SalesOrder> salesOrders) {
        // Region is stored on SalesOrder, not RunRecord, so we co-walk both lists by orderId.
        Map<String, SalesOrder> byOrder = new LinkedHashMap<>();
        for (SalesOrder so : salesOrders) byOrder.put(so.id(), so);

        Map<String, BucketAcc> byKey = new LinkedHashMap<>();
        for (RunRecord r : records) {
            if (r.error() != null) continue;
            SalesOrder so = byOrder.get(r.orderId());
            String region = so == null ? "UNKNOWN" : so.region();
            BigDecimal usd = so == null ? BigDecimal.ZERO : so.grandTotalUsd();
            byKey.computeIfAbsent(region, k -> new BucketAcc()).add(r, usd);
        }
        return new BatchSummary("Region", finalise(byKey));
    }

    private static BatchSummary rollup(String dimension,
                                       List<RunRecord> records,
                                       List<SalesOrder> salesOrders,
                                       java.util.function.Function<RunRecord, String> keyFn) {
        Map<String, SalesOrder> byOrder = new LinkedHashMap<>();
        for (SalesOrder so : salesOrders) byOrder.put(so.id(), so);

        Map<String, BucketAcc> byKey = new LinkedHashMap<>();
        for (RunRecord r : records) {
            if (r.error() != null) continue;
            SalesOrder so = byOrder.get(r.orderId());
            BigDecimal usd = so == null ? BigDecimal.ZERO : so.grandTotalUsd();
            String key = keyFn.apply(r);
            if (key == null || key.isEmpty()) key = "UNKNOWN";
            byKey.computeIfAbsent(key, k -> new BucketAcc()).add(r, usd);
        }
        return new BatchSummary(dimension, finalise(byKey));
    }

    private static List<Bucket> finalise(Map<String, BucketAcc> byKey) {
        List<Bucket> out = new ArrayList<>(byKey.size());
        byKey.forEach((k, acc) -> out.add(acc.toBucket(k)));
        return out;
    }

    private static final class BucketAcc {
        int orderCount;
        int itemCount;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalUsd = BigDecimal.ZERO;

        void add(RunRecord r, BigDecimal usd) {
            orderCount++;
            itemCount += r.itemCount();
            total = total.add(r.grandTotal());
            totalUsd = totalUsd.add(usd);
        }

        Bucket toBucket(String key) {
            return new Bucket(key, orderCount, itemCount,
                    total.setScale(2, RoundingMode.HALF_UP),
                    totalUsd.setScale(2, RoundingMode.HALF_UP));
        }
    }
}
