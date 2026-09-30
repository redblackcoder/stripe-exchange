package com.stripe.currency.exchange.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.currency.exchange.api.FinanceApi;
import com.stripe.currency.exchange.model.Currency;
import com.stripe.currency.exchange.model.Transaction;
import com.stripe.currency.exchange.service.TransactionService;

@RestController
public class TransactionController implements FinanceApi {
    @Autowired
    private TransactionService service;

    @Override
    public ResponseEntity<List<Transaction>> listTransactions(Currency currency) {
        try {
            var transactions = service.getTransactions(currency);
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Void> createTransaction(Transaction transaction) {
        try {
            service.add(transaction);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
