package com.stripe.currency.exchange.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.stripe.currency.exchange.http.HttpException;
import com.stripe.currency.exchange.http.JsonClient;
import com.stripe.currency.exchange.model.Currency;
import com.stripe.currency.exchange.service.IExchangeRate;

@Component
public class Frankfurter implements IExchangeRate {
    private static final String url = "https://api.frankfurter.dev/";

    private final Set<String> fetched;
    private final Map<String, Double> rate;
    private final JsonClient client;
    private final int retries;

    public Frankfurter() {
        fetched = new HashSet<>();
        rate = new HashMap<>();
        client = new JsonClient(url);
        retries = 3;
    }

    @Override
    public Optional<Double> conversion(Currency from, Currency to) throws Exception {
        if (!fetched.contains(cacheKey(from.getValue(), null))) {
            fetchLatestRates(from.getValue());
        }

        return Optional.ofNullable(rate.get(buildKey(from, to, null)));
    }

    @Override
    public Optional<Double> conversion(Currency from, Currency to, LocalDate date) throws Exception {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!fetched.contains(cacheKey(from.getValue(), dateStr))) {
            fetchRates(from.getValue(), dateStr);
        }

        return Optional.ofNullable(rate.get(buildKey(from, to, dateStr)));
    }

    private void fetchLatestRates(String base) throws Exception {
        fetchRates(base, null);
    }

    private void fetchRates(String base, String date) throws Exception {
        Rates rates = null;
        for (int i = 0; i < retries; i++) {
            try {
                if (date == null || date.length() == 0) {
                    rates = client.get("v1/latest", Map.of("base", base), Rates.class);
                } else {
                    rates = client.get("v1/" + date, Map.of("base", base), Rates.class);
                }
                break;
            } catch (HttpException he) {
                if (he.getStatusCode() / 100 == 5) {
                    System.err.printf("Error while fetching rates. Msg: %s\n", he.getMessage());
                    if (i == retries - 1) {
                        throw new Exception("All attempts to fetch rates failed.");
                    }

                    System.out.printf("Retrying. Attempt %d\n", i + 1);
                } else if (he.getStatusCode() == 404) {
                    throw new IllegalArgumentException("Unknown Currency Code: " + base);
                } else {
                    throw new Exception("Non-retriable error while fetchin rates.");
                }
            }
        }

        fetched.add(cacheKey(base, rates.date));
        final Rates fRates = rates;
        rates.rates.forEach((to, val) -> {
            rate.put(buildKey(base, to, fRates.date), val);
        });
    }

    private static String buildKey(Currency from, Currency to, String date) {
        return buildKey(from.getValue(), to.getValue(), date);
    }

    private static String buildKey(String from, String to, String date) {
        if (date == null || date.length() == 0) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        return from + "-" + to + "-" + date;
    }

    private static String cacheKey(String base, String date) {
        if (date == null || date.length() == 0) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        return base + "-" + date;
    }
}
