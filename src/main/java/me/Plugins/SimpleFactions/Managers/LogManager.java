package me.Plugins.SimpleFactions.Managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Buffered debug log written to {@code logs/log.txt} in the plugin data folder.
 * Enable via {@code logging: true} in config.yml.
 */
public final class LogManager {
	private static final Logger LOGGER = Logger.getLogger(LogManager.class.getName());
	private static final DateTimeFormatter SESSION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	static final String LOG_DIRECTORY = "logs";
	static final String LOG_FILE_NAME = "log.txt";

	private static volatile boolean enabled;
	private static volatile Path logFile;
	private static final Object LOCK = new Object();
	private static final List<String> sessionLines = new ArrayList<>();

	private LogManager() {
	}

	public static void configure(boolean loggingEnabled, boolean wipeLog, File dataFolder) {
		enabled = loggingEnabled;
		if (dataFolder != null) {
			logFile = dataFolder.toPath().resolve(LOG_DIRECTORY).resolve(LOG_FILE_NAME);
		}
		if (wipeLog) {
			wipeLogFile();
		}
	}

	private static void wipeLogFile() {
		if (logFile == null) {
			return;
		}
		synchronized (LOCK) {
			sessionLines.clear();
			try {
				Files.deleteIfExists(logFile);
			} catch (IOException exception) {
				LOGGER.warning("Failed to wipe log.txt: " + exception.getMessage());
			}
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void beginSession(String title) {
		if (!enabled) {
			return;
		}
		synchronized (LOCK) {
			sessionLines.clear();
			sessionLines.add("===== " + SESSION_TIME.format(Instant.now()) + " " + title + " =====");
		}
	}

	public static void section(String header) {
		if (!enabled) {
			return;
		}
		synchronized (LOCK) {
			sessionLines.add("");
			sessionLines.add("--- " + header + " ---");
		}
	}

	public static void line(String message) {
		if (!enabled || message == null) {
			return;
		}
		synchronized (LOCK) {
			sessionLines.add(message);
		}
	}

	public static void line(String format, Object... args) {
		if (!enabled) {
			return;
		}
		line(String.format(format, args));
	}

	/** Append a single line immediately (no session buffer). */
	public static void append(String message) {
		if (!enabled || message == null || logFile == null) {
			return;
		}
		writeLines(List.of(SESSION_TIME.format(Instant.now()) + " " + message), false);
	}

	public static void flush() {
		if (!enabled || logFile == null) {
			return;
		}
		synchronized (LOCK) {
			if (sessionLines.isEmpty()) {
				return;
			}
			writeLines(sessionLines, true);
			sessionLines.clear();
		}
	}

	private static void writeLines(List<String> lines, boolean trailingBlankLine) {
		try {
			Files.createDirectories(logFile.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(
					logFile,
					StandardCharsets.UTF_8,
					StandardOpenOption.CREATE,
					StandardOpenOption.APPEND)) {
				for (String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				if (trailingBlankLine) {
					writer.newLine();
				}
			}
		} catch (IOException exception) {
			LOGGER.warning("Failed to write log.txt: " + exception.getMessage());
		}
	}
}
