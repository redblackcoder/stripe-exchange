package com.stripe.currency.exchange.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stripe.currency.exchange.model.Currency;
import com.stripe.currency.exchange.model.Transaction;

@Service
public class TransactionService {
    private static final LocalDate CUTOFF_DATE = LocalDate.of(1990, 1, 1);

    private final Map<String, Transaction> store;
    private final IExchangeRate exchangeRate;

    @Autowired
    public TransactionService(IExchangeRate exchangeRate) {
        store = new ConcurrentHashMap<>();
        this.exchangeRate = exchangeRate;
    }

    public List<Transaction> getTransactions(Currency currency) throws Exception {
        List<Transaction> res = new ArrayList<>();
        for (var e : store.entrySet()) {
            String tid = e.getKey();
            var t = e.getValue();
            var transaction = new Transaction();
            transaction.id(tid);
            transaction.currency(currency);
            transaction.date(t.getDate());
            double convertedAmount = convert(t.getAmount(), t.getCurrency(), currency, t.getDate());
            transaction.amount(convertedAmount);
            res.add(transaction);
        }

        return res;
    }

    public void add(Transaction transaction) {
        validateInput(transaction);

        String tid = UUID.randomUUID().toString();
        var storedTxn = new Transaction();
        storedTxn.id(tid);
        storedTxn.amount(transaction.getAmount());
        storedTxn.currency(transaction.getCurrency());
        storedTxn.date(transaction.getDate() == null ? LocalDate.now() : transaction.getDate());
        store.put(tid, storedTxn);
    }

    private void validateInput(Transaction txn) {
        if (txn.getAmount() == null || txn.getAmount() < 0) {
            String msg;
            if (txn.getAmount() == null) {
                msg = "Amount cannot be null";
            } else {
                msg = String.format("Invalid amount %.4f", txn.getAmount());
            }

            System.err.println(msg);
            throw new IllegalArgumentException("Transaction amount should be positive");
        }

        if (txn.getDate() == null) {
            System.out.printf("Transaction date not set. Will be set to %s", LocalDate.now());
            return;
        }

        if (txn.getDate().isBefore(CUTOFF_DATE)) {
            System.err.println("Invalid date, it is older than CUTOFF date");
            throw new IllegalArgumentException("Transaction date should be on of after " + CUTOFF_DATE);
        }

        if (txn.getDate().isAfter(LocalDate.now())) {
            System.err.println("Invalid date, it is in future");
            throw new IllegalArgumentException("Transaction date cannot be in future");
        }
    }

    private double convert(
            double amount,
            Currency from,
            Currency to,
            LocalDate date) throws Exception {
        if (from == to) {
            return amount;
        }

        Optional<Double> rateOp = date == null
                ? exchangeRate.conversion(from, to)
                : exchangeRate.conversion(from, to, date);
        if (rateOp.isEmpty()) {
            throw new Exception("Cannot exhange rate between " + from.getValue() + " and " + to.getValue());
        }

        return amount * rateOp.get();
    }
}
