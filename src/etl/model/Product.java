package etl.model;

import java.math.BigDecimal;

/**
 * Product master entry loaded from {@code data/reference/product_master.csv}.
 * Used for inventory weight calculation and tax-category lookup.
 */
public record Product(String sku,
                      String category,
                      BigDecimal weightKgPerUnit,
                      String taxCategory) {

    public static final Product UNKNOWN =
            new Product("UNKNOWN", "UNKNOWN", BigDecimal.ZERO, "UNKNOWN");
}
