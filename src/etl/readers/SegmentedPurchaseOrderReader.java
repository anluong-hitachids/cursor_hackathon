package etl.readers;

import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads an EDIFACT/IDoc-style segmented legacy file where each line is a
 * pipe-delimited segment whose first field is the segment type.
 *
 * <pre>
 *   HDR|orderId|customer|orderDate|currency
 *   LIN|sku|qty|unitPrice
 *   LIN|sku|qty|unitPrice
 *   HDR|...
 *   LIN|...
 * </pre>
 *
 * A new {@code HDR} segment begins a new {@link PurchaseOrder};
 * subsequent {@code LIN} segments are attached to it.
 */
public final class SegmentedPurchaseOrderReader {

    private static final String DELIM = "\\|";

    private SegmentedPurchaseOrderReader() {}

    public static List<PurchaseOrder> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        List<PurchaseOrder> orders = new ArrayList<>();
        String currentOrderId = null, currentCustomer = null,
               currentDate = null, currentCurrency = null;
        List<POLine> currentLines = null;

        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] f = line.split(DELIM, -1);
            String segment = f[0];

            switch (segment) {
                case "HDR" -> {
                    if (currentOrderId != null) {
                        orders.add(new PurchaseOrder(currentOrderId, currentCustomer,
                                currentDate, currentCurrency, currentLines));
                    }
                    if (f.length < 5) {
                        throw new IOException("Malformed HDR segment at line " + lineNo
                                + ": expected 5 fields, got " + f.length);
                    }
                    currentOrderId  = f[1];
                    currentCustomer = f[2];
                    currentDate     = f[3];
                    currentCurrency = f[4];
                    currentLines    = new ArrayList<>();
                }
                case "LIN" -> {
                    if (currentLines == null) {
                        throw new IOException("LIN segment at line " + lineNo
                                + " before any HDR segment");
                    }
                    if (f.length < 4) {
                        throw new IOException("Malformed LIN segment at line " + lineNo
                                + ": expected 4 fields, got " + f.length);
                    }
                    currentLines.add(new POLine(
                            f[1],
                            Integer.parseInt(f[2].trim()),
                            new BigDecimal(f[3].trim())));
                }
                default ->
                    throw new IOException("Unknown segment type at line " + lineNo + ": " + segment);
            }
        }

        if (currentOrderId != null) {
            orders.add(new PurchaseOrder(currentOrderId, currentCustomer,
                    currentDate, currentCurrency, currentLines));
        }
        return orders;
    }
}
