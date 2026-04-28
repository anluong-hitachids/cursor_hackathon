package etl.readers;

import etl.model.PurchaseOrder;
import etl.model.PurchaseOrder.POLine;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Reads a single BizTalk-style purchase order XML file into a {@link PurchaseOrder}. */
public final class XmlPurchaseOrderReader {

    private XmlPurchaseOrderReader() {}

    public static PurchaseOrder read(Path file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(file.toFile());
        doc.getDocumentElement().normalize();

        Element root   = doc.getDocumentElement();
        Element header = (Element) root.getElementsByTagName("Header").item(0);

        String orderId   = textOf(header, "OrderId");
        String customer  = textOf(header, "Customer");
        String orderDate = textOf(header, "OrderDate");
        String currency  = textOf(header, "Currency");

        List<POLine> lines = new ArrayList<>();
        NodeList lineNodes = root.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element line = (Element) lineNodes.item(i);
            lines.add(new POLine(
                    textOf(line, "Sku"),
                    Integer.parseInt(textOf(line, "Qty")),
                    new BigDecimal(textOf(line, "UnitPrice"))));
        }
        return new PurchaseOrder(orderId, customer, orderDate, currency, lines);
    }

    private static String textOf(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() == 0 ? "" : nl.item(0).getTextContent().trim();
    }
}
