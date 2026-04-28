package etl.mappers;

import etl.model.Product;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads {@code product_master.csv} once and exposes a case-insensitive
 * lookup by SKU, returning {@link Product#UNKNOWN} on miss.
 */
public final class ProductLookup {

    private final Map<String, Product> bySku;

    private ProductLookup(Map<String, Product> bySku) {
        this.bySku = bySku;
    }

    public static ProductLookup load(Path masterCsv) throws IOException {
        if (!Files.exists(masterCsv)) {
            return new ProductLookup(Map.of());
        }
        List<String> lines = Files.readAllLines(masterCsv, StandardCharsets.UTF_8);
        if (lines.size() < 2) return new ProductLookup(Map.of());

        String[] header = lines.get(0).split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        for (String required : List.of("Sku","Category","WeightKgPerUnit","TaxCategory")) {
            if (!col.containsKey(required)) {
                throw new IOException("product_master.csv missing column: " + required);
            }
        }

        Map<String, Product> map = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row.isBlank()) continue;
            String[] f = row.split(",", -1);

            String sku = f[col.get("Sku")].trim();
            map.put(sku.toLowerCase(Locale.ROOT), new Product(
                    sku,
                    f[col.get("Category")].trim(),
                    new BigDecimal(f[col.get("WeightKgPerUnit")].trim()),
                    f[col.get("TaxCategory")].trim()));
        }
        return new ProductLookup(map);
    }

    public Product find(String sku) {
        if (sku == null) return Product.UNKNOWN;
        Product p = bySku.get(sku.trim().toLowerCase(Locale.ROOT));
        return p != null ? p : Product.UNKNOWN;
    }

    public boolean contains(String sku) {
        return sku != null && bySku.containsKey(sku.trim().toLowerCase(Locale.ROOT));
    }

    public int size() {
        return bySku.size();
    }
}
