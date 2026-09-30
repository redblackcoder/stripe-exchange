package com.stripe.currency.exchange.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonClient {
    private final HttpClient client;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final ObjectMapper mapper;

    public JsonClient(String baseUrl) {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = baseUrl;
        requestTimeout = Duration.ofSeconds(10);
        mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    JsonClient(String baseUrl, HttpClient client) {
        this.client = client;
        this.baseUrl = baseUrl;
        requestTimeout = Duration.ofSeconds(10);
        mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public <T> T get(String path, Map<String, String> params, Class<T> clazz) throws HttpException {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path + buildQueryParams(params)))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), clazz);
            } else {
                throw new HttpException(response.body(), response.statusCode());
            }
        } catch (IOException ioe) {
            // log error
            throw new HttpException(
                    String.format("Failed to fetch responce for %s. Err: %s", request.uri(), ioe.getMessage()),
                    0);
        } catch (InterruptedException inte) {
            // log error
            Thread.currentThread().interrupt();
        }

        return null;
    }

    private String buildQueryParams(Map<String, String> params) {
        String encoded = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return encoded.length() == 0 ? "" : "?" + encoded;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
