package etl.mappers;

import etl.model.Customer;
import etl.model.Invoice;
import etl.model.Invoice.InvoiceLine;
import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Derives an {@link Invoice} from a {@link SalesOrder} by applying the
 * customer's tax rate and payment terms.
 *
 * <ul>
 *   <li>{@code subtotal} = sales-order grand total</li>
 *   <li>{@code taxAmount} = subtotal &times; customer.taxRate</li>
 *   <li>{@code totalDue} = subtotal + taxAmount</li>
 *   <li>{@code dueDate} = today + customer.paymentTermsDays</li>
 * </ul>
 */
public final class SalesOrderToInvoice {

    private SalesOrderToInvoice() {}

    public static Invoice map(SalesOrder so, Customer customer, LocalDate today) {

        List<InvoiceLine> lines = so.items().stream()
                .map(SalesOrderToInvoice::toInvoiceLine)
                .toList();

        BigDecimal subtotal  = so.grandTotal();
        BigDecimal taxRate   = customer.taxRate();
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDue  = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new Invoice(
                "INV-" + so.id(),
                so.id(),
                so.customerName(),
                so.customerCode(),
                so.currency(),
                today,
                today.plusDays(customer.paymentTermsDays()),
                customer.paymentTermsDays(),
                lines,
                subtotal,
                taxRate,
                taxAmount,
                totalDue);
    }

    private static InvoiceLine toInvoiceLine(SalesItem item) {
        return new InvoiceLine(item.productCode(), item.quantity(),
                item.price(), item.lineTotal());
    }
}
