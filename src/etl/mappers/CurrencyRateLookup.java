package etl.mappers;

import etl.model.CurrencyRate;

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
 * Loads {@code currency_rates.csv} (Currency, RateToUSD) into a
 * case-insensitive map.  Used by {@code PurchaseOrderToSalesOrder}
 * to populate {@code grandTotalUsd} on every {@code SalesOrder}.
 */
public final class CurrencyRateLookup {

    private final Map<String, CurrencyRate> byCurrency;

    private CurrencyRateLookup(Map<String, CurrencyRate> byCurrency) {
        this.byCurrency = byCurrency;
    }

    public static CurrencyRateLookup load(Path csv) throws IOException {
        if (!Files.exists(csv)) {
            return new CurrencyRateLookup(Map.of("USD", CurrencyRate.USD));
        }
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            return new CurrencyRateLookup(Map.of("USD", CurrencyRate.USD));
        }

        String[] header = lines.get(0).split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        for (String required : List.of("Currency", "RateToUSD")) {
            if (!col.containsKey(required)) {
                throw new IOException("currency_rates.csv missing column: " + required);
            }
        }

        Map<String, CurrencyRate> map = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row.isBlank()) continue;
            String[] f = row.split(",", -1);

            String code = f[col.get("Currency")].trim();
            map.put(code.toUpperCase(Locale.ROOT), new CurrencyRate(
                    code,
                    new BigDecimal(f[col.get("RateToUSD")].trim())));
        }
        // Always ensure USD is present
        map.putIfAbsent("USD", CurrencyRate.USD);
        return new CurrencyRateLookup(map);
    }

    public boolean isSupported(String currency) {
        return currency != null && byCurrency.containsKey(currency.trim().toUpperCase(Locale.ROOT));
    }

    public CurrencyRate find(String currency) {
        if (currency == null) return CurrencyRate.USD;
        CurrencyRate r = byCurrency.get(currency.trim().toUpperCase(Locale.ROOT));
        return r != null ? r : CurrencyRate.USD;
    }

    public int size() {
        return byCurrency.size();
    }
}
