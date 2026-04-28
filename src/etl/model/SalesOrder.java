package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canonical target model produced by mapping a {@link PurchaseOrder}
 * against the {@link Customer} reference data.
 *
 * <p>Downstream maps ({@code SalesOrder -> Invoice}, {@code SalesOrder -> Shipment},
 * {@code SalesOrder -> InventoryAdjustment}) read from this canonical type
 * rather than from the original source.</p>
 *
 * <p>{@code grandTotal} is in the order's own {@code currency};
 * {@code grandTotalUsd} is the FX-converted amount in the base (USD) currency.</p>
 */
public record SalesOrder(String batchId,
                         String processedAt,
                         String id,
                         String customerName,
                         String customerCode,
                         String region,
                         String orderDate,
                         String currency,
                         BigDecimal fxRateToUsd,
                         List<SalesItem> items,
                         int itemCount,
                         BigDecimal grandTotal,
                         BigDecimal grandTotalUsd) {

    public record SalesItem(String productCode,
                            int quantity,
                            BigDecimal price,
                            BigDecimal lineTotal) {}
}
