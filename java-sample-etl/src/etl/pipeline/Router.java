package etl.pipeline;

import etl.model.SalesOrder;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Content-based router: decides whether a {@link SalesOrder} is "high value"
 * (and therefore routed to a different output folder) using a single
 * configurable threshold.
 */
public final class Router {

    private final BigDecimal highValueThreshold;
    private final Path highValueDir;
    private final Path standardDir;

    public Router(BigDecimal highValueThreshold, Path highValueDir, Path standardDir) {
        this.highValueThreshold = highValueThreshold;
        this.highValueDir       = highValueDir;
        this.standardDir        = standardDir;
    }

    public boolean isHighValue(SalesOrder so) {
        return so.grandTotal().compareTo(highValueThreshold) >= 0;
    }

    public Path destinationFor(SalesOrder so) {
        return isHighValue(so) ? highValueDir : standardDir;
    }

    public BigDecimal threshold() {
        return highValueThreshold;
    }
}
