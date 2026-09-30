package com.stripe.currency.exchange.service;

import java.util.Optional;

import com.stripe.currency.exchange.model.Currency;

public interface IExchangeRate {
    public Optional<Double> conversion(Currency from, Currency to) throws Exception;
}
