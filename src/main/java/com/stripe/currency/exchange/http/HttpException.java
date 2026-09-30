package com.stripe.currency.exchange.http;

public class HttpException extends Exception {
    private final int statusCode;

    public HttpException(String msg, int statusCode) {
        super("Http Status Code: " + statusCode + " | Error: " + msg);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
