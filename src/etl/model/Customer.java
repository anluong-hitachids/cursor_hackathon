package etl.model;

import java.math.BigDecimal;

/**
 * Reference-data entity loaded from {@code data/reference/customer_master.csv}.
 * Used to enrich a {@link PurchaseOrder} during mapping.
 */
public record Customer(String name,
                       String code,
                       int paymentTermsDays,
                       BigDecimal taxRate,
                       String region) {

    /** Fallback used when a purchase order's customer is not in the master file. */
    public static final Customer UNKNOWN =
            new Customer("UNKNOWN", "UNKNOWN", 30, BigDecimal.ZERO, "UNKNOWN");
}
