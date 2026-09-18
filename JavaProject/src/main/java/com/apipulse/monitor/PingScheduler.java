package com.apipulse.monitor;

import com.apipulse.model.Endpoint;
import com.apipulse.model.HealthResult;
import com.apipulse.repository.LogRepository;
import com.apipulse.service.HttpRequestService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runs recurring endpoint checks on background scheduled threads. */
public final class PingScheduler implements AutoCloseable {
    private final HttpRequestService requestService;
    private final LogRepository logRepository;
    private ScheduledExecutorService executor;

    /**
     * Creates a scheduler connected to the HTTP and persistence services.
     *
     * @param requestService service used to perform health checks
     * @param logRepository repository used to store results
     */
    public PingScheduler(HttpRequestService requestService, LogRepository logRepository) {
        this.requestService = requestService;
        this.logRepository = logRepository;
    }

    /**
     * Starts one recurring task for each endpoint.
     *
     * @param endpoints endpoints to check; an empty list starts no useful tasks
     */
    public synchronized void start(List<Endpoint> endpoints) {
        stop();
        executor = Executors.newScheduledThreadPool(Math.max(1, endpoints.size()));
        for (Endpoint endpoint : endpoints) {
            long intervalSeconds = endpoint.interval().toSeconds();
            executor.scheduleAtFixedRate(() -> checkAndLog(endpoint), 0,
                    Math.max(1, intervalSeconds), TimeUnit.SECONDS);
        }
    }

    /** Stops all scheduled checks and interrupts their worker threads. */
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * Checks whether scheduled monitoring is active.
     *
     * @return {@code true} when the scheduler has an active executor
     */
    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    private void checkAndLog(Endpoint endpoint) {
        HealthResult result = requestService.check(endpoint);
        logRepository.append(result);
        String status = result.statusCode() == -1 ? result.detail() :
                result.statusCode() + (result.passed() ? " OK" : " ERR");
        String color = result.passed() ? "\u001B[32m" : "\u001B[31m";
        synchronized (System.out) {
            System.out.printf("%s[%s] %s %s | %s | %d ms | %s%s%n",
                    color,
                    java.time.LocalTime.now().withNano(0),
                    endpoint.method(), endpoint.uri(), status,
                    result.responseTimeMs(), result.statusLabel(), "\u001B[0m");
        }
    }

    /** Stops monitoring when the scheduler is used in a try-with-resources block. */
    @Override
    public void close() {
        stop();
    }
}
