package etl.writers;

import etl.model.Invoice;
import etl.model.Invoice.InvoiceLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static etl.writers.XmlSupport.appendText;
import static etl.writers.XmlSupport.newDocument;
import static etl.writers.XmlSupport.writePretty;

/** Serialises an {@link Invoice} into XML. */
public final class InvoiceXmlWriter {

    private InvoiceXmlWriter() {}

    public static void write(Invoice inv, Path target) throws Exception {
        Document doc = newDocument();

        Element root = doc.createElement("Invoice");
        root.setAttribute("invoiceNumber", inv.invoiceNumber());
        doc.appendChild(root);

        appendText(doc, root, "OrderId",          inv.orderId());
        appendText(doc, root, "CustomerName",     inv.customerName());
        appendText(doc, root, "CustomerCode",     inv.customerCode());
        appendText(doc, root, "Currency",         inv.currency());
        appendText(doc, root, "InvoiceDate",      inv.invoiceDate().toString());
        appendText(doc, root, "DueDate",          inv.dueDate().toString());
        appendText(doc, root, "PaymentTermsDays", Integer.toString(inv.paymentTermsDays()));

        Element lines = doc.createElement("Lines");
        root.appendChild(lines);
        for (InvoiceLine line : inv.lines()) {
            Element l = doc.createElement("Line");
            appendText(doc, l, "ProductCode", line.productCode());
            appendText(doc, l, "Quantity",    Integer.toString(line.quantity()));
            appendText(doc, l, "Price",       line.price().toPlainString());
            appendText(doc, l, "LineTotal",   line.lineTotal().toPlainString());
            lines.appendChild(l);
        }

        BigDecimal taxRatePct = inv.taxRate()
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        Element totals = doc.createElement("Totals");
        appendText(doc, totals, "Subtotal",   inv.subtotal().toPlainString());
        appendText(doc, totals, "TaxRatePct", taxRatePct.toPlainString());
        appendText(doc, totals, "TaxAmount",  inv.taxAmount().toPlainString());
        appendText(doc, totals, "TotalDue",   inv.totalDue().toPlainString());
        root.appendChild(totals);

        writePretty(doc, target);
    }
}
