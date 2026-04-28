package etl.mappers;

import etl.model.InventoryAdjustment;
import etl.model.InventoryAdjustment.AdjustmentLine;
import etl.model.Product;
import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Derives an {@link InventoryAdjustment} from a {@link SalesOrder} using
 * the product master to look up real per-unit weights and categories.
 */
public final class SalesOrderToInventoryAdjustment {

    private SalesOrderToInventoryAdjustment() {}

    public static InventoryAdjustment map(SalesOrder so, ProductLookup products) {

        List<AdjustmentLine> lines = so.items().stream()
                .map(item -> toAdjustmentLine(item, products))
                .toList();

        BigDecimal totalWeight = lines.stream()
                .map(AdjustmentLine::weightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new InventoryAdjustment(
                "ADJ-" + so.id(),
                so.id(),
                so.customerName(),
                "ORDER_FULFILMENT",
                totalWeight,
                lines);
    }

    private static AdjustmentLine toAdjustmentLine(SalesItem item, ProductLookup products) {
        Product p = products.find(item.productCode());
        BigDecimal weight = p.weightKgPerUnit()
                .multiply(BigDecimal.valueOf(item.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new AdjustmentLine(item.productCode(), p.category(),
                item.quantity(), weight);
    }
}
