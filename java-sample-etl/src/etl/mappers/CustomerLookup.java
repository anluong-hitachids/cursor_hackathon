package etl.mappers;

import etl.model.Customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads {@code customer_master.csv} once and exposes a case-insensitive
 * lookup by customer name, returning {@link Customer#UNKNOWN} on miss.
 *
 * <p>Required columns (in any order):
 * {@code CustomerName,CustomerCode,PaymentTermsDays,TaxRate,Region}.</p>
 */
public final class CustomerLookup {

    private final Map<String, Customer> byName;

    private CustomerLookup(Map<String, Customer> byName) {
        this.byName = byName;
    }

    public static CustomerLookup load(Path masterCsv) throws IOException {
        if (!Files.exists(masterCsv)) {
            return new CustomerLookup(Map.of());
        }
        List<String> lines = Files.readAllLines(masterCsv, StandardCharsets.UTF_8);
        if (lines.size() < 2) return new CustomerLookup(Map.of());

        String[] header = lines.get(0).split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        for (String required :
                List.of("CustomerName","CustomerCode","PaymentTermsDays","TaxRate","Region")) {
            if (!col.containsKey(required)) {
                throw new IOException("customer_master.csv missing column: " + required);
            }
        }

        Map<String, Customer> map = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row.isBlank()) continue;
            String[] f = row.split(",", -1);

            String name = f[col.get("CustomerName")].trim();
            Customer c = new Customer(
                    name,
                    f[col.get("CustomerCode")].trim(),
                    Integer.parseInt(f[col.get("PaymentTermsDays")].trim()),
                    new BigDecimal(f[col.get("TaxRate")].trim()),
                    f[col.get("Region")].trim());
            map.put(name.toLowerCase(Locale.ROOT), c);
        }
        return new CustomerLookup(map);
    }

    public Customer find(String customerName) {
        if (customerName == null) return Customer.UNKNOWN;
        Customer c = byName.get(customerName.trim().toLowerCase(Locale.ROOT));
        return c != null ? c : Customer.UNKNOWN;
    }

    public int size() {
        return byName.size();
    }
}
