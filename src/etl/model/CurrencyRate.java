package etl.model;

import java.math.BigDecimal;

/**
 * One row from {@code data/reference/currency_rates.csv}: how much one unit
 * of {@code currency} is worth in the base currency (USD).
 */
public record CurrencyRate(String currency, BigDecimal rateToUsd) {

    public static final CurrencyRate USD = new CurrencyRate("USD", BigDecimal.ONE);
}
