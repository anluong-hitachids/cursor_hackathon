package etl.writers;

import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serialises a {@link SalesOrder} into a pretty-printed JSON document. */
public final class SalesOrderJsonWriter {

    private SalesOrderJsonWriter() {}

    public static void write(SalesOrder so, Path target) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("batchId",       so.batchId());
        root.put("processedAt",   so.processedAt());
        root.put("id",            so.id());
        root.put("customerName",  so.customerName());
        root.put("customerCode",  so.customerCode());
        root.put("region",        so.region());
        root.put("orderDate",     so.orderDate());
        root.put("currency",      so.currency());
        root.put("fxRateToUsd",   so.fxRateToUsd());

        List<Map<String, Object>> items = new ArrayList<>(so.items().size());
        for (SalesItem item : so.items()) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("productCode", item.productCode());
            i.put("quantity",    item.quantity());
            i.put("price",       item.price());
            i.put("lineTotal",   item.lineTotal());
            items.add(i);
        }
        root.put("items", items);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("itemCount",      so.itemCount());
        totals.put("grandTotal",     so.grandTotal());
        totals.put("grandTotalUsd",  so.grandTotalUsd());
        root.put("totals", totals);

        JsonSupport.writePretty(root, target);
    }
}
