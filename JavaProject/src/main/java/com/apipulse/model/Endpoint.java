package com.apipulse.model;

import java.net.URI;
import java.time.Duration;

/** Stores the configuration for one monitored API endpoint. */
public record Endpoint(String name, URI uri, String method, Duration interval) {
    /**
     * Validates and creates an endpoint configuration.
     *
     * @param name display name for the endpoint
     * @param uri target API URI
     * @param method HTTP method used by health checks; currently only GET is supported
     * @param interval delay between health checks
     * @throws IllegalArgumentException if any endpoint value is invalid
     */
    public Endpoint {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Endpoint name cannot be blank.");
        }
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Endpoint URL must include a valid host.");
        }
        if (!method.equalsIgnoreCase("GET")) {
            throw new IllegalArgumentException("Health monitoring currently supports GET endpoints.");
        }
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Interval must be greater than zero.");
        }
        method = method.toUpperCase();
    }
}
