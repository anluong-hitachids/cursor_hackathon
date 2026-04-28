package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Derived logistics document produced from a {@link SalesOrder}.
 * Carrier and service level are chosen by simple business rules
 * (see {@code etl.mappers.SalesOrderToShipment}).
 */
public record Shipment(String shipmentNumber,
                       String orderId,
                       String customerName,
                       String region,
                       String carrier,
                       String serviceLevel,
                       BigDecimal estimatedWeightKg,
                       int packageCount,
                       List<ShipmentItem> items) {

    public record ShipmentItem(String productCode,
                               int quantity,
                               BigDecimal estimatedWeightKg) {}
}
