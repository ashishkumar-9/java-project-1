package com.apipulse.ui;

import com.apipulse.model.Endpoint;
import com.apipulse.monitor.PingScheduler;
import com.apipulse.repository.LogRepository;
import com.apipulse.service.HttpRequestService;
import com.apipulse.service.HttpRequestService.ResponseData;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Provides the interactive terminal menu for API-Pulse. */
public final class ConsoleUi {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    private final Scanner scanner;
    private final List<Endpoint> endpoints = new ArrayList<>();
    private final HttpRequestService requestService;
    private final PingScheduler scheduler;
    private final LogRepository logRepository;

    /**
     * Creates the console and connects it to the application services.
     *
     * @param requestService service used for manual API requests
     * @param scheduler service used for background monitoring
     * @param logRepository repository used for log export
     */
    public ConsoleUi(HttpRequestService requestService, PingScheduler scheduler, LogRepository logRepository) {
        scanner = new Scanner(System.in);
        this.requestService = requestService;
        this.scheduler = scheduler;
        this.logRepository = logRepository;
    }

    /** Runs the menu loop until the user selects Exit. */
    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String option = prompt("Select option");
            try {
                switch (option) {
                    case "1" -> addEndpoint();
                    case "2" -> startMonitoring();
                    case "3" -> quickRequest();
                    case "4" -> exportLogs();
                    case "5" -> running = false;
                    default -> System.out.println("Choose an option from 1 to 5.");
                }
            } catch (RuntimeException exception) {
                System.out.println("Could not complete that action: " + exception.getMessage());
            }
        }
        scheduler.stop();
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println("\n" + CYAN + "=== API HEALTH PULSE CLI ===" + RESET);
        System.out.println("1. Add Endpoint");
        System.out.println("2. Start Live Monitoring");
        System.out.println("3. Quick API Request Test");
        System.out.println("4. Export Logs to File");
        System.out.println("5. Exit");
        if (!endpoints.isEmpty()) {
            System.out.println(YELLOW + "Registered endpoints: " + endpoints.size() + RESET);
        }
    }

    private void addEndpoint() {
        String name = prompt("Endpoint name");
        URI uri = URI.create(prompt("URL (https://... )"));
        long seconds = Long.parseLong(prompt("Ping interval in seconds"));
        endpoints.add(new Endpoint(name, uri, "GET", Duration.ofSeconds(seconds)));
        System.out.println("Endpoint registered.");
    }

    private void startMonitoring() {
        if (endpoints.isEmpty()) {
            System.out.println("Add at least one endpoint first.");
            return;
        }
        scheduler.start(endpoints);
        System.out.println("\n" + CYAN + "[MONITORING STARTED - Press ENTER to stop]" + RESET);
        scanner.nextLine();
        scheduler.stop();
        System.out.println("Monitoring stopped.");
    }

    private void quickRequest() {
        String url = prompt("URL");
        String method = prompt("HTTP method (GET/POST)").toUpperCase();
        if (!method.equals("GET") && !method.equals("POST")) {
            System.out.println("Only GET and POST are supported.");
            return;
        }
        String headerInput = prompt("Headers (Name: Value, comma-separated; blank for none)");
        String body = method.equals("POST") ? prompt("Request body") : "";
        ResponseData response = requestService.request(url, method,
                HttpRequestService.parseHeaders(headerInput), body);
        if (response.error() != null) {
            System.out.println("Request failed: " + response.error());
            return;
        }
        System.out.printf("%nStatus: %d | %d ms%n%s%n", response.statusCode(),
                response.responseTimeMs(), response.body());
    }

    private void exportLogs() {
        try {
            Path export = logRepository.exportSnapshot(Path.of("health_history_export.csv"));
            System.out.println("Exported " + logRepository.size() + " in-memory records to " + export.toAbsolutePath());
        } catch (Exception exception) {
            System.out.println("Could not export logs: " + exception.getMessage());
        }
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
