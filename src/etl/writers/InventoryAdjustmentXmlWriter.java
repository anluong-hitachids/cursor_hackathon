package etl.writers;

import etl.model.InventoryAdjustment;
import etl.model.InventoryAdjustment.AdjustmentLine;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static etl.writers.XmlSupport.appendText;
import static etl.writers.XmlSupport.newDocument;
import static etl.writers.XmlSupport.writePretty;

/** Serialises an {@link InventoryAdjustment} into XML. */
public final class InventoryAdjustmentXmlWriter {

    private InventoryAdjustmentXmlWriter() {}

    public static void write(InventoryAdjustment adj, Path target) throws Exception {
        Document doc = newDocument();

        Element root = doc.createElement("InventoryAdjustment");
        root.setAttribute("adjustmentNumber", adj.adjustmentNumber());
        doc.appendChild(root);

        appendText(doc, root, "OrderId",       adj.orderId());
        appendText(doc, root, "CustomerName",  adj.customerName());
        appendText(doc, root, "ReasonCode",    adj.reasonCode());
        appendText(doc, root, "TotalWeightKg", adj.totalWeightKg().toPlainString());

        Element lines = doc.createElement("Lines");
        root.appendChild(lines);
        for (AdjustmentLine line : adj.lines()) {
            Element l = doc.createElement("Line");
            appendText(doc, l, "Sku",              line.sku());
            appendText(doc, l, "Category",         line.category());
            appendText(doc, l, "QuantityDeducted", Integer.toString(line.quantityDeducted()));
            appendText(doc, l, "WeightKg",         line.weightKg().toPlainString());
            lines.appendChild(l);
        }

        writePretty(doc, target);
    }
}
