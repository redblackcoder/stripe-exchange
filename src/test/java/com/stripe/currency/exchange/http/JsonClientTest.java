package com.stripe.currency.exchange.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.stripe.currency.exchange.client.Rates;

public class JsonClientTest {
    private static final String RATES = """
            {
              "amount": 1,
              "base": "USD",
              "date": "2026-09-29",
              "rates": {
                "AUD": 1.4277,
                "CAD": 1.418,
                "EUR": 0.88067,
                "INR": 95.98
              }
            }
            """;

    @Test
    public void get_success() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<Object> mockResponse = mock(HttpResponse.class);
        when(mockClient.send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(RATES);

        JsonClient jsonClient = new JsonClient("http://foo.com/rates/", mockClient);
        Rates rates = jsonClient.get("", Map.of("base", "USD"), Rates.class);
        assertEquals("USD", rates.base);
        assertEquals("2026-09-29", rates.date);
        assertEquals(4, rates.rates.size());
        assertTrue(numbersEqual(1.418, rates.rates.get("CAD")));
    }

    private static boolean numbersEqual(double x, double y) {
        return Math.abs(x - y) < 1e-5;
    }
}
