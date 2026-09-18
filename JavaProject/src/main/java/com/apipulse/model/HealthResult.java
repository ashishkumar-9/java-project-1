package com.apipulse.model;

import java.time.Instant;

/** Represents the outcome of one API health check. */
public record HealthResult(
        Instant checkedAt,
        Endpoint endpoint,
        int statusCode,
        long responseTimeMs,
        boolean passed,
        String detail) {

    /**
     * Converts the boolean result into the label shown in the CLI.
     *
     * @return {@code PASS} for a successful check, otherwise {@code FAIL}
     */
    public String statusLabel() {
        return passed ? "PASS" : "FAIL";
    }
}
