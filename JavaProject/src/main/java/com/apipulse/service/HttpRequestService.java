package com.apipulse.service;

import com.apipulse.model.Endpoint;
import com.apipulse.model.HealthResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executes HTTP health checks and manual API requests. */
public final class HttpRequestService {
    private static final int ERROR_STATUS = -1;
    private final HttpClient client;

    /** Creates an HTTP client with connection and redirect settings. */
    public HttpRequestService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Sends a GET request and converts the response or failure into a health result.
     *
     * @param endpoint endpoint to check
     * @return health result containing status, latency, and failure details
     */
    public HealthResult check(Endpoint endpoint) {
        Instant started = Instant.now();
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint.uri())
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "API-Pulse-CLI/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = elapsedMs(started);
            boolean passed = response.statusCode() >= 200 && response.statusCode() < 400;
            return new HealthResult(started, endpoint, response.statusCode(), elapsed, passed,
                    passed ? "OK" : "HTTP error");
        } catch (Exception exception) {
            return new HealthResult(started, endpoint, ERROR_STATUS, elapsedMs(started), false,
                    friendlyError(exception));
        }
    }

    /**
     * Sends a manual GET or POST request.
     *
     * @param url destination URL
     * @param method HTTP method, either GET or POST
     * @param headers request headers to attach
     * @param body request body used for POST requests
     * @return response status, latency, payload, or a friendly error message
     */
    public ResponseData request(String url, String method, Map<String, String> headers, String body) {
        Instant started = Instant.now();
        try {
            URI uri = URI.create(url);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20));
            headers.forEach(builder::header);
            if (method.equalsIgnoreCase("POST")) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ResponseData(response.statusCode(), elapsedMs(started), response.body(), null);
        } catch (Exception exception) {
            return new ResponseData(ERROR_STATUS, elapsedMs(started), "", friendlyError(exception));
        }
    }

    /**
     * Parses comma-separated {@code Name: Value} header pairs.
     *
     * @param input header text entered by the user
     * @return parsed headers, or an empty map when the input is blank
     */
    public static Map<String, String> parseHeaders(String input) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (input == null || input.isBlank()) {
            return headers;
        }
        for (String item : input.split(",")) {
            String[] parts = item.split(":", 2);
            if (parts.length == 2 && !parts[0].isBlank()) {
                headers.put(parts[0].trim(), parts[1].trim());
            }
        }
        return headers;
    }

    private long elapsedMs(Instant started) {
        return Duration.between(started, Instant.now()).toMillis();
    }

    private String friendlyError(Exception exception) {
        if (exception instanceof java.net.http.HttpTimeoutException) {
            return "Request timed out";
        }
        if (exception instanceof IllegalArgumentException) {
            return "Invalid URL";
        }
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    /** Holds the result of a manual HTTP request. */
    public record ResponseData(int statusCode, long responseTimeMs, String body, String error) {
    }
}
