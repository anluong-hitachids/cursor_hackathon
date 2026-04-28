package etl.mappers;

import etl.model.Customer;
import etl.model.CurrencyRate;
import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;
import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Maps a source {@link PurchaseOrder} into a canonical {@link SalesOrder}.
 *
 * <ul>
 *   <li>Enriches customer fields from {@link CustomerLookup}.</li>
 *   <li>Computes per-line totals and grand total in the order's own currency.</li>
 *   <li>Computes {@code grandTotalUsd} via {@link CurrencyRateLookup}.</li>
 * </ul>
 */
public final class PurchaseOrderToSalesOrder {

    private PurchaseOrderToSalesOrder() {}

    public static SalesOrder map(PurchaseOrder po,
                                 CustomerLookup customers,
                                 CurrencyRateLookup currencies,
                                 String batchId,
                                 String processedAt) {

        Customer     customer = customers.find(po.customer());
        CurrencyRate rate     = currencies.find(po.currency());

        List<SalesItem> items = po.lines().stream()
                .map(PurchaseOrderToSalesOrder::toSalesItem)
                .toList();

        BigDecimal grandTotal = items.stream()
                .map(SalesItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotalUsd = grandTotal
                .multiply(rate.rateToUsd())
                .setScale(2, RoundingMode.HALF_UP);

        return new SalesOrder(
                batchId,
                processedAt,
                po.orderId(),
                normalize(po.customer()),
                customer.code(),
                customer.region(),
                po.orderDate(),
                po.currency(),
                rate.rateToUsd(),
                items,
                items.size(),
                grandTotal,
                grandTotalUsd);
    }

    private static SalesItem toSalesItem(POLine line) {
        BigDecimal lineTotal = line.unitPrice()
                .multiply(BigDecimal.valueOf(line.qty()))
                .setScale(2, RoundingMode.HALF_UP);
        return new SalesItem(line.sku(), line.qty(), line.unitPrice(), lineTotal);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
