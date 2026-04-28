package etl.validators;

import etl.mappers.CurrencyRateLookup;
import etl.mappers.ProductLookup;
import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;
import etl.model.ValidationIssue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a {@link PurchaseOrder} against a fixed set of business rules.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>{@code E001} - Order must have at least one line.</li>
 *   <li>{@code E002} - Every line quantity must be a positive integer.</li>
 *   <li>{@code E003} - Every line unit price must be &gt;= 0.</li>
 *   <li>{@code E004} - Currency must be present in the currency_rates lookup.</li>
 *   <li>{@code E005} - OrderDate must be parseable as ISO local date.</li>
 *   <li>{@code W001} - Warning if a SKU is unknown to the product master.</li>
 *   <li>{@code W002} - Warning if line quantity exceeds 10 000 (sanity check).</li>
 * </ul>
 *
 * <p>Orders with at least one {@code ERROR} are sent to a quarantine folder;
 * warnings are recorded in the audit log but processing proceeds.</p>
 */
public final class OrderValidator {

    private static final BigDecimal MAX_REASONABLE_QTY = new BigDecimal("10000");

    private final ProductLookup products;
    private final CurrencyRateLookup currencies;

    public OrderValidator(ProductLookup products, CurrencyRateLookup currencies) {
        this.products   = products;
        this.currencies = currencies;
    }

    public List<ValidationIssue> validate(PurchaseOrder po) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (po.lines() == null || po.lines().isEmpty()) {
            issues.add(ValidationIssue.error("E001", "Order has no lines"));
        }

        if (!currencies.isSupported(po.currency())) {
            issues.add(ValidationIssue.error("E004",
                    "Unsupported currency: " + po.currency()));
        }

        try {
            LocalDate.parse(po.orderDate());
        } catch (DateTimeParseException ex) {
            issues.add(ValidationIssue.error("E005",
                    "Unparseable orderDate: " + po.orderDate()));
        } catch (NullPointerException ex) {
            issues.add(ValidationIssue.error("E005", "Missing orderDate"));
        }

        if (po.lines() != null) {
            int li = 0;
            for (POLine line : po.lines()) {
                li++;
                if (line.qty() <= 0) {
                    issues.add(ValidationIssue.error("E002",
                            "Line " + li + " (sku=" + line.sku() + ") has non-positive qty: " + line.qty()));
                } else if (BigDecimal.valueOf(line.qty()).compareTo(MAX_REASONABLE_QTY) > 0) {
                    issues.add(ValidationIssue.warning("W002",
                            "Line " + li + " qty " + line.qty() + " exceeds sanity threshold "
                                    + MAX_REASONABLE_QTY));
                }
                if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
                    issues.add(ValidationIssue.error("E003",
                            "Line " + li + " (sku=" + line.sku() + ") has negative unit price"));
                }
                if (!products.contains(line.sku())) {
                    issues.add(ValidationIssue.warning("W001",
                            "Line " + li + " unknown SKU: " + line.sku()));
                }
            }
        }

        return issues;
    }

    public static boolean hasErrors(List<ValidationIssue> issues) {
        return issues.stream().anyMatch(i -> i.severity() == ValidationIssue.Severity.ERROR);
    }
}
