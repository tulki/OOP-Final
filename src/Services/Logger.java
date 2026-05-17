package Services;

import Enums.LogEventType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static Logger instance;
    private static final String LOG_FILE = "data/logs.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PrintWriter writer;

    private Logger() {
        try {
            new File("data").mkdirs();
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("Logger init failed: " + e.getMessage());
        }
    }

    public static Logger getInstance() {
        if (instance == null) instance = new Logger();
        return instance;
    }

    public void log(LogEventType type, String details) {
        String line = String.format("[%s] [%-15s] %s",
                LocalDateTime.now().format(FMT), type, details);
        if (writer != null) writer.println(line);
    }

    public void close() {
        if (writer != null) writer.close();
    }
}
