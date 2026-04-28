package etl.pipeline;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * One row in the run report.  Captures everything that happened to a single
 * source document so we can write the audit log and CSV summary at the end.
 */
public record RunRecord(String source,
                        String orderId,
                        String customer,
                        String customerCode,
                        String currency,
                        int itemCount,
                        BigDecimal grandTotal,
                        boolean isHighValue,
                        Path salesOrderFile,
                        Path invoiceFile,
                        Path shipmentFile,
                        String carrier,
                        String error) {

    public static RunRecord success(String source,
                                    String orderId,
                                    String customer,
                                    String customerCode,
                                    String currency,
                                    int itemCount,
                                    BigDecimal grandTotal,
                                    boolean isHighValue,
                                    Path salesOrderFile,
                                    Path invoiceFile,
                                    Path shipmentFile,
                                    String carrier) {
        return new RunRecord(source, orderId, customer, customerCode, currency,
                itemCount, grandTotal, isHighValue,
                salesOrderFile, invoiceFile, shipmentFile, carrier, null);
    }

    public static RunRecord failure(String source, String kind, Exception ex) {
        return new RunRecord(source, "?", "?", "?", "?",
                0, BigDecimal.ZERO, false,
                null, null, null, null,
                kind + " error: " + ex.getMessage());
    }

    public String routedTo() {
        if (error != null)  return "ERROR";
        return isHighValue ? "HIGH_VALUE" : "STANDARD";
    }
}
