package etl.writers;

import etl.model.SalesOrder;
import etl.model.SalesOrder.SalesItem;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static etl.writers.XmlSupport.appendText;
import static etl.writers.XmlSupport.newDocument;
import static etl.writers.XmlSupport.writePretty;

/** Serialises a {@link SalesOrder} into a canonical XML document. */
public final class SalesOrderXmlWriter {

    private SalesOrderXmlWriter() {}

    public static void write(SalesOrder so, Path target) throws Exception {
        Document doc = newDocument();

        Element root = doc.createElement("SalesOrder");
        root.setAttribute("batchId",     so.batchId());
        root.setAttribute("processedAt", so.processedAt());
        doc.appendChild(root);

        appendText(doc, root, "ID",            so.id());
        appendText(doc, root, "CustomerName",  so.customerName());
        appendText(doc, root, "CustomerCode",  so.customerCode());
        appendText(doc, root, "Region",        so.region());
        appendText(doc, root, "OrderDate",     so.orderDate());
        appendText(doc, root, "Currency",      so.currency());

        Element items = doc.createElement("Items");
        root.appendChild(items);
        for (SalesItem item : so.items()) {
            Element it = doc.createElement("Item");
            appendText(doc, it, "ProductCode", item.productCode());
            appendText(doc, it, "Quantity",    Integer.toString(item.quantity()));
            appendText(doc, it, "Price",       item.price().toPlainString());
            appendText(doc, it, "LineTotal",   item.lineTotal().toPlainString());
            items.appendChild(it);
        }

        Element totals = doc.createElement("Totals");
        appendText(doc, totals, "ItemCount",  Integer.toString(so.itemCount()));
        appendText(doc, totals, "GrandTotal", so.grandTotal().toPlainString());
        root.appendChild(totals);

        writePretty(doc, target);
    }
}
