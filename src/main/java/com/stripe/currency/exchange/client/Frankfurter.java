package com.stripe.currency.exchange.client;

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
        if (!fetched.contains(from.getValue())) {
            fetchRates(from.getValue());
        }

        return Optional.ofNullable(rate.get(buildKey(from, to)));
    }

    private void fetchRates(String base) throws Exception {
        Rates rates = null;
        for (int i = 0; i < retries; i++) {
            try {
                rates = client.get("v1/latest", Map.of("base", base), Rates.class);
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

        fetched.add(base);
        rates.rates.forEach((to, val) -> {
            rate.put(buildKey(base, to), val);
        });
    }

    private static String buildKey(Currency from, Currency to) {
        return buildKey(from.getValue(), to.getValue());
    }

    private static String buildKey(String from, String to) {
        return from + "-" + to;
    }
}
