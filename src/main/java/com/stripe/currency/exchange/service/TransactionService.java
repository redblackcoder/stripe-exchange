package com.stripe.currency.exchange.service;

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
            double convertedAmount = convert(t.getAmount(), t.getCurrency(), currency);
            transaction.amount(convertedAmount);
            res.add(transaction);
        }

        return res;
    }

    public void add(Transaction transaction) {
        String tid = UUID.randomUUID().toString();
        var storedTxn = new Transaction();
        storedTxn.id(tid);
        storedTxn.amount(transaction.getAmount());
        storedTxn.currency(transaction.getCurrency());
        store.put(tid, storedTxn);
    }

    private double convert(double amount, Currency from, Currency to) throws Exception {
        Optional<Double> rateOp = exchangeRate.conversion(from, to);
        if (rateOp.isEmpty()) {
            throw new Exception("Cannot exhange rate between " + from.getValue() + " and " + to.getValue());
        }

        return amount * rateOp.get();
    }
}
