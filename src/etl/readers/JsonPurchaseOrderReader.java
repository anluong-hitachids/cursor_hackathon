package etl.readers;

import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a JSON file containing an array of purchase orders.  Expected shape:
 *
 * <pre>
 * [
 *   {
 *     "orderId":   "PO-####",
 *     "customer":  "Some Co.",
 *     "orderDate": "YYYY-MM-DD",
 *     "currency":  "USD",
 *     "lines": [
 *       { "sku": "...", "qty": 10, "unitPrice": 12.34 }
 *     ]
 *   }
 * ]
 * </pre>
 */
public final class JsonPurchaseOrderReader {

    private JsonPurchaseOrderReader() {}

    public static List<PurchaseOrder> read(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Object root = MiniJson.parse(text);
        if (!(root instanceof List<?> array)) {
            throw new IOException("Expected top-level JSON array in " + file);
        }

        List<PurchaseOrder> orders = new ArrayList<>(array.size());
        int idx = 0;
        for (Object obj : array) {
            idx++;
            if (!(obj instanceof Map<?, ?> map)) {
                throw new IOException("Order #" + idx + " is not a JSON object");
            }
            String orderId   = str(map, "orderId",   "Order #" + idx);
            String customer  = str(map, "customer",  orderId);
            String orderDate = str(map, "orderDate", orderId);
            String currency  = str(map, "currency",  orderId);

            Object linesObj = map.get("lines");
            if (!(linesObj instanceof List<?> lineArr)) {
                throw new IOException("Order " + orderId + ": 'lines' is missing or not an array");
            }
            List<POLine> lines = new ArrayList<>(lineArr.size());
            int li = 0;
            for (Object lo : lineArr) {
                li++;
                if (!(lo instanceof Map<?, ?> lm)) {
                    throw new IOException("Order " + orderId + " line #" + li + " not an object");
                }
                lines.add(new POLine(
                        str(lm, "sku", orderId + "/line " + li),
                        Integer.parseInt(num(lm, "qty",        orderId + "/line " + li)),
                        new BigDecimal(  num(lm, "unitPrice",  orderId + "/line " + li))));
            }
            orders.add(new PurchaseOrder(orderId, customer, orderDate, currency, lines));
        }
        return orders;
    }

    private static String str(Map<?, ?> m, String key, String ctx) throws IOException {
        Object v = m.get(key);
        if (v == null) throw new IOException(ctx + ": missing string field '" + key + "'");
        return v.toString().trim();
    }

    private static String num(Map<?, ?> m, String key, String ctx) throws IOException {
        Object v = m.get(key);
        if (v == null) throw new IOException(ctx + ": missing numeric field '" + key + "'");
        return v.toString().trim();
    }
}
