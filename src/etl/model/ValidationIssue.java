package etl.model;

/**
 * One problem found by a validator.  Severity {@code ERROR} sends the order
 * to quarantine; {@code WARNING} is logged but doesn't block processing.
 */
public record ValidationIssue(Severity severity, String code, String message) {

    public enum Severity { WARNING, ERROR }

    public static ValidationIssue error(String code, String message) {
        return new ValidationIssue(Severity.ERROR, code, message);
    }

    public static ValidationIssue warning(String code, String message) {
        return new ValidationIssue(Severity.WARNING, code, message);
    }
}
