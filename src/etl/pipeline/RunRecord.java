package etl.pipeline;

import etl.model.ValidationIssue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

/**
 * One row in the run report.  Captures everything that happened to a single
 * source document so we can write the audit log and CSV summary at the end.
 *
 * <p>Statuses: {@code ACCEPTED} (passed validation, written),
 * {@code QUARANTINED} (validation errors, written to quarantine folder),
 * {@code FAILED} (read or processing exception).</p>
 */
public record RunRecord(String source,
                        String orderId,
                        String customer,
                        String customerCode,
                        String currency,
                        int itemCount,
                        BigDecimal grandTotal,
                        BigDecimal grandTotalUsd,
                        boolean isHighValue,
                        Status status,
                        Path salesOrderXmlFile,
                        Path salesOrderJsonFile,
                        Path invoiceFile,
                        Path shipmentFile,
                        Path inventoryAdjustmentFile,
                        Path quarantineFile,
                        String carrier,
                        List<ValidationIssue> issues,
                        String error) {

    public enum Status { ACCEPTED, QUARANTINED, FAILED }

    public static RunRecord accepted(String source,
                                     String orderId,
                                     String customer,
                                     String customerCode,
                                     String currency,
                                     int itemCount,
                                     BigDecimal grandTotal,
                                     BigDecimal grandTotalUsd,
                                     boolean isHighValue,
                                     Path salesOrderXmlFile,
                                     Path salesOrderJsonFile,
                                     Path invoiceFile,
                                     Path shipmentFile,
                                     Path inventoryAdjustmentFile,
                                     String carrier,
                                     List<ValidationIssue> warnings) {
        return new RunRecord(source, orderId, customer, customerCode, currency,
                itemCount, grandTotal, grandTotalUsd, isHighValue, Status.ACCEPTED,
                salesOrderXmlFile, salesOrderJsonFile, invoiceFile, shipmentFile,
                inventoryAdjustmentFile, null, carrier, warnings, null);
    }

    public static RunRecord quarantined(String source,
                                        String orderId,
                                        String customer,
                                        Path quarantineFile,
                                        List<ValidationIssue> issues) {
        return new RunRecord(source, orderId, customer, "?", "?",
                0, BigDecimal.ZERO, BigDecimal.ZERO, false, Status.QUARANTINED,
                null, null, null, null, null, quarantineFile, null, issues,
                "Validation failed: " + issues.size() + " issue(s)");
    }

    public static RunRecord failure(String source, String kind, Exception ex) {
        return new RunRecord(source, "?", "?", "?", "?",
                0, BigDecimal.ZERO, BigDecimal.ZERO, false, Status.FAILED,
                null, null, null, null, null, null, null, List.of(),
                kind + " error: " + ex.getMessage());
    }

    public String routedTo() {
        if (status == Status.FAILED)      return "ERROR";
        if (status == Status.QUARANTINED) return "QUARANTINE";
        return isHighValue ? "HIGH_VALUE" : "STANDARD";
    }

    public boolean isAccepted()    { return status == Status.ACCEPTED;    }
    public boolean isQuarantined() { return status == Status.QUARANTINED; }
    public boolean isFailure()     { return status == Status.FAILED;     }
}
