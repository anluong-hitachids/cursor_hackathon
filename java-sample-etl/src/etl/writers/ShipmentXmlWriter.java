package etl.writers;

import etl.model.Shipment;
import etl.model.Shipment.ShipmentItem;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static etl.writers.XmlSupport.appendText;
import static etl.writers.XmlSupport.newDocument;
import static etl.writers.XmlSupport.writePretty;

/** Serialises a {@link Shipment} into XML. */
public final class ShipmentXmlWriter {

    private ShipmentXmlWriter() {}

    public static void write(Shipment ship, Path target) throws Exception {
        Document doc = newDocument();

        Element root = doc.createElement("Shipment");
        root.setAttribute("shipmentNumber", ship.shipmentNumber());
        doc.appendChild(root);

        appendText(doc, root, "OrderId",          ship.orderId());
        appendText(doc, root, "CustomerName",     ship.customerName());
        appendText(doc, root, "Region",           ship.region());
        appendText(doc, root, "Carrier",          ship.carrier());
        appendText(doc, root, "ServiceLevel",     ship.serviceLevel());
        appendText(doc, root, "EstimatedWeightKg", ship.estimatedWeightKg().toPlainString());
        appendText(doc, root, "PackageCount",     Integer.toString(ship.packageCount()));

        Element items = doc.createElement("Items");
        root.appendChild(items);
        for (ShipmentItem item : ship.items()) {
            Element it = doc.createElement("Item");
            appendText(doc, it, "ProductCode",        item.productCode());
            appendText(doc, it, "Quantity",           Integer.toString(item.quantity()));
            appendText(doc, it, "EstimatedWeightKg",  item.estimatedWeightKg().toPlainString());
            items.appendChild(it);
        }

        writePretty(doc, target);
    }
}
