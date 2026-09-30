package com.stripe.currency.exchange.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.stripe.currency.exchange.model.Currency;
import com.stripe.currency.exchange.model.Transaction;

public class TransactionServiceTest {
    @Test
    public void add_get_same_currency() throws Exception {
        var service = new TransactionService(new MockExchangeRate());
        service.add(new Transaction().amount(100.0).currency(Currency.USD));
        List<Transaction> txns = service.getTransactions(Currency.USD);
        assertEquals(1, txns.size());
        assertEquals(Currency.USD, txns.get(0).getCurrency());
        assertTrue(numbersEqual(100.0, txns.get(0).getAmount()));
    }

    @Test
    public void add_get_diff_currency() throws Exception {
        var service = new TransactionService(new MockExchangeRate());
        service.add(new Transaction().amount(100.0).currency(Currency.CAD));
        List<Transaction> txns = service.getTransactions(Currency.USD);
        assertEquals(1, txns.size());
        assertEquals(Currency.USD, txns.get(0).getCurrency());
        assertTrue(numbersEqual(88.0, txns.get(0).getAmount()));
    }

    private static boolean numbersEqual(double x, double y) {
        return Math.abs(x - y) < 1e-5;
    }

    private static class MockExchangeRate implements IExchangeRate {
        private static final Map<String, Double> rates = Map.of(
                "USD-CAD", 1.45,
                "CAD-USD", 0.88);

        @Override
        public Optional<Double> conversion(Currency from, Currency to) throws Exception {
            return Optional.ofNullable(rates.get(String.format("%s-%s", from.getValue(), to.getValue())));
        }
    }
}
