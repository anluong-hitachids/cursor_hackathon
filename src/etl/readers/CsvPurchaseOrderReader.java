package etl.readers;

import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a CSV file where rows are grouped by {@code OrderId} into one
 * {@link PurchaseOrder} per group.  Required header:
 * {@code OrderId,Customer,OrderDate,Currency,Sku,Qty,UnitPrice}.
 */
public final class CsvPurchaseOrderReader {

    private static final List<String> REQUIRED_COLS = List.of(
            "OrderId", "Customer", "OrderDate", "Currency", "Sku", "Qty", "UnitPrice");

    private CsvPurchaseOrderReader() {}

    public static List<PurchaseOrder> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() < 2) return List.of();

        String[] header = splitCsv(lines.get(0));
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        for (String required : REQUIRED_COLS) {
            if (!col.containsKey(required)) {
                throw new IOException("CSV missing required column: " + required
                        + "  (found: " + col.keySet() + ")");
            }
        }

        Map<String, List<String[]>> grouped = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row.isBlank()) continue;
            String[] f = splitCsv(row);
            grouped.computeIfAbsent(f[col.get("OrderId")], k -> new ArrayList<>()).add(f);
        }

        List<PurchaseOrder> result = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> e : grouped.entrySet()) {
            List<String[]> rows = e.getValue();
            String[] first = rows.get(0);
            List<POLine> poLines = new ArrayList<>();
            for (String[] r : rows) {
                poLines.add(new POLine(
                        r[col.get("Sku")],
                        Integer.parseInt(r[col.get("Qty")].trim()),
                        new BigDecimal(r[col.get("UnitPrice")].trim())));
            }
            result.add(new PurchaseOrder(
                    first[col.get("OrderId")],
                    first[col.get("Customer")],
                    first[col.get("OrderDate")],
                    first[col.get("Currency")],
                    poLines));
        }
        return result;
    }

    /** Minimal CSV splitter: no quoted-field support needed for the demo data. */
    private static String[] splitCsv(String line) {
        return line.split(",", -1);
    }
}
