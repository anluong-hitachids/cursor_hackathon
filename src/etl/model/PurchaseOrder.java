package etl.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Source-side domain model: a BizTalk-style purchase order.
 * One {@code PurchaseOrder} can come from any of the supported source formats
 * (XML, CSV, segmented legacy file).
 */
public record PurchaseOrder(String orderId,
                            String customer,
                            String orderDate,
                            String currency,
                            List<POLine> lines) {

    public record POLine(String sku, int qty, BigDecimal unitPrice) {}
}
