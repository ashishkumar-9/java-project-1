# API-Pulse CLI

A dependency-free Java 17 command-line API health monitor.

## Features

- Register GET endpoints with independent ping intervals.
- Monitor endpoints concurrently with `ScheduledExecutorService`.
- Run manual GET and POST requests with custom headers.
- Append health results to `health_history.csv`.
- Export the current log snapshot to `health_history_export.csv`.
- Graceful handling for invalid URLs, timeouts, and network failures.

## Run with Maven

```maven
mvn compile exec:java
```

## Run with the JDK directly

PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | Select-Object -ExpandProperty FullName)
java -cp out com.apipulse.App
```

The CSV files are created in the project directory when the application runs.
