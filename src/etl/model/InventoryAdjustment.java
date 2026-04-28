package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stock-deduction document derived from a {@link SalesOrder}.
 * Each adjustment records the SKU, quantity to deduct, and total weight
 * removed from inventory.
 */
public record InventoryAdjustment(String adjustmentNumber,
                                  String orderId,
                                  String customerName,
                                  String reasonCode,
                                  BigDecimal totalWeightKg,
                                  List<AdjustmentLine> lines) {

    public record AdjustmentLine(String sku,
                                 String category,
                                 int quantityDeducted,
                                 BigDecimal weightKg) {}
}
