package etl.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Derived financial document produced from a {@link SalesOrder} by applying
 * the customer's tax rate and payment terms.
 */
public record Invoice(String invoiceNumber,
                      String orderId,
                      String customerName,
                      String customerCode,
                      String currency,
                      LocalDate invoiceDate,
                      LocalDate dueDate,
                      int paymentTermsDays,
                      List<InvoiceLine> lines,
                      BigDecimal subtotal,
                      BigDecimal taxRate,
                      BigDecimal taxAmount,
                      BigDecimal totalDue) {

    public record InvoiceLine(String productCode,
                              int quantity,
                              BigDecimal price,
                              BigDecimal lineTotal) {}
}
