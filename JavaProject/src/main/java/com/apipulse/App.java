package com.apipulse;

import com.apipulse.monitor.PingScheduler;
import com.apipulse.repository.LogRepository;
import com.apipulse.service.HttpRequestService;
import com.apipulse.ui.ConsoleUi;

import java.nio.file.Path;

/** Starts the API-Pulse command-line application. */
public final class App {
    private App() {
    }

    /**
     * Creates the application services and starts the interactive console.
     *
     * @param args command-line arguments; currently unused
     * @throws Exception if the health history file cannot be initialized
     */
    public static void main(String[] args) throws Exception {
        LogRepository logRepository = new LogRepository(Path.of("health_history.csv"));
        HttpRequestService requestService = new HttpRequestService();
        try (PingScheduler scheduler = new PingScheduler(requestService, logRepository)) {
            new ConsoleUi(requestService, scheduler, logRepository).run();
        }
    }
}
