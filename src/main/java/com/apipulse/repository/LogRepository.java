package com.apipulse.repository;

import com.apipulse.model.HealthResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Persists health check results as CSV records. */
public final class LogRepository {
    private static final DateTimeFormatter CSV_TIME =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private final Path historyFile;
    private final List<HealthResult> history = new CopyOnWriteArrayList<>();

    /**
     * Opens or creates the health history file.
     *
     * @param historyFile file used to store health results
     * @throws IOException if the file or its CSV header cannot be created
     */
    public LogRepository(Path historyFile) throws IOException {
        this.historyFile = historyFile;
        ensureHeader();
    }

    /**
     * Adds a result to memory and appends it to the CSV file.
     *
     * @param result health check result to persist
     */
    public synchronized void append(HealthResult result) {
        history.add(result);
        try (BufferedWriter writer = Files.newBufferedWriter(
                historyFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(toCsv(result));
            writer.newLine();
        } catch (IOException exception) {
            System.err.println("Could not write health log: " + exception.getMessage());
        }
    }

    /**
     * Copies the current history file to another path.
     *
     * @param exportFile destination file for the exported CSV
     * @return the destination path
     * @throws IOException if the history cannot be copied
     */
    public synchronized Path exportSnapshot(Path exportFile) throws IOException {
        Files.copy(historyFile, exportFile, StandardCopyOption.REPLACE_EXISTING);
        return exportFile;
    }

    /**
     * Reports how many results were appended during this application run.
     *
     * @return number of in-memory health results
     */
    public int size() {
        return history.size();
    }

    /**
     * Returns the configured history file path.
     *
     * @return health history file path
     */
    public Path historyFile() {
        return historyFile;
    }

    private void ensureHeader() throws IOException {
        if (Files.notExists(historyFile) || Files.size(historyFile) == 0) {
            Files.writeString(historyFile,
                    "timestamp,endpoint,method,status_code,response_time_ms,result,detail\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }

    private String toCsv(HealthResult result) {
        return String.join(",",
                quote(CSV_TIME.format(result.checkedAt())),
                quote(result.endpoint().uri().toString()),
                result.endpoint().method(),
                Integer.toString(result.statusCode()),
                Long.toString(result.responseTimeMs()),
                result.statusLabel(),
                quote(result.detail()));
    }

    private String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
