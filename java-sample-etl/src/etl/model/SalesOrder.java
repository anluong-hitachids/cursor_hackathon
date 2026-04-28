package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canonical target model produced by mapping a {@link PurchaseOrder}
 * against the {@link Customer} reference data.
 *
 * <p>Downstream maps ({@code SalesOrder -> Invoice}, {@code SalesOrder -> Shipment})
 * read from this canonical type rather than from the original source.</p>
 */
public record SalesOrder(String batchId,
                         String processedAt,
                         String id,
                         String customerName,
                         String customerCode,
                         String region,
                         String orderDate,
                         String currency,
                         List<SalesItem> items,
                         int itemCount,
                         BigDecimal grandTotal) {

    public record SalesItem(String productCode,
                            int quantity,
                            BigDecimal price,
                            BigDecimal lineTotal) {}
}
