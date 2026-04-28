package etl.mappers;

import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;
import etl.model.Shipment;
import etl.model.Shipment.ShipmentItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Derives a {@link Shipment} from a {@link SalesOrder}.
 *
 * <p>Carrier selection rules (illustrative):</p>
 * <ul>
 *   <li>Order total &ge; 50 000  &nbsp;&rarr; FEDEX, PRIORITY_OVERNIGHT</li>
 *   <li>Order total &ge; 5 000   &nbsp;&rarr; FEDEX, EXPRESS_2DAY</li>
 *   <li>Region starts with "EU"  &nbsp;&rarr; DHL, EUROPE_GROUND</li>
 *   <li>Region starts with "UK"  &nbsp;&rarr; ROYAL_MAIL, TRACKED_24</li>
 *   <li>Otherwise                &nbsp;&rarr; UPS, GROUND</li>
 * </ul>
 *
 * <p>Estimated weight is a simple {@code 0.5 kg} per unit (demo placeholder).
 * Package count is one package per 50 kg, minimum 1.</p>
 */
public final class SalesOrderToShipment {

    private static final BigDecimal WEIGHT_PER_UNIT_KG = new BigDecimal("0.50");
    private static final BigDecimal KG_PER_PACKAGE     = new BigDecimal("50");
    private static final BigDecimal HIGH_VAL_TIER      = new BigDecimal("50000.00");
    private static final BigDecimal MID_VAL_TIER       = new BigDecimal("5000.00");

    private SalesOrderToShipment() {}

    public static Shipment map(SalesOrder so) {

        List<ShipmentItem> items = so.items().stream()
                .map(SalesOrderToShipment::toShipmentItem)
                .toList();

        BigDecimal totalWeight = items.stream()
                .map(ShipmentItem::estimatedWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int packageCount = totalWeight.divide(KG_PER_PACKAGE, 0, RoundingMode.UP).intValue();
        if (packageCount < 1) packageCount = 1;

        String[] carrierAndService = chooseCarrier(so.grandTotal(), so.region());

        return new Shipment(
                "SHIP-" + so.id(),
                so.id(),
                so.customerName(),
                so.region(),
                carrierAndService[0],
                carrierAndService[1],
                totalWeight,
                packageCount,
                items);
    }

    private static ShipmentItem toShipmentItem(SalesItem item) {
        BigDecimal weight = WEIGHT_PER_UNIT_KG
                .multiply(BigDecimal.valueOf(item.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new ShipmentItem(item.productCode(), item.quantity(), weight);
    }

    private static String[] chooseCarrier(BigDecimal grandTotal, String region) {
        if (grandTotal.compareTo(HIGH_VAL_TIER) >= 0) {
            return new String[]{ "FEDEX", "PRIORITY_OVERNIGHT" };
        }
        if (grandTotal.compareTo(MID_VAL_TIER) >= 0) {
            return new String[]{ "FEDEX", "EXPRESS_2DAY" };
        }
        if (region != null && region.startsWith("EU")) {
            return new String[]{ "DHL", "EUROPE_GROUND" };
        }
        if (region != null && region.startsWith("UK")) {
            return new String[]{ "ROYAL_MAIL", "TRACKED_24" };
        }
        return new String[]{ "UPS", "GROUND" };
    }
}
