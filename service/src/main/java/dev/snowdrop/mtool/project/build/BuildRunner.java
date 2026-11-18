package dev.snowdrop.mtool.project.build;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a build command in a project directory with a timeout and extracts the relevant error lines
 * from its output.
 */
public final class BuildRunner {

    private static final Logger logger = Logger.getLogger(BuildRunner.class);

    /** Default timeout of a build run by a validator. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private static final int MAX_ERROR_LINES = 5;
    private static final int MAX_FALLBACK_CHARS = 500;

    public enum Outcome {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        INTERRUPTED,
        NOT_RUNNABLE
    }

    /**
     * @param outcome result of the build
     * @param details error lines on failure, or the reason why the build could not run
     */
    public record Result(Outcome outcome, String details) {
    }

    private BuildRunner() {
    }

    /** Maven command, using the project's wrapper when present, in batch mode without transfer progress. */
    public static List<String> mavenCommand(Path projectDir, String... goals) {
        List<String> command = new ArrayList<>();
        command.add(executable(projectDir, "mvnw", "mvn"));
        command.addAll(List.of(goals));
        command.addAll(List.of("-B", "--no-transfer-progress"));
        return command;
    }

    /** Gradle command, using the project's wrapper when present. */
    public static List<String> gradleCommand(Path projectDir, String... tasks) {
        List<String> command = new ArrayList<>();
        command.add(executable(projectDir, "gradlew", "gradle"));
        command.addAll(List.of(tasks));
        return command;
    }

    private static String executable(Path projectDir, String wrapper, String fallback) {
        return Files.isExecutable(projectDir.resolve(wrapper)) ? "./" + wrapper : fallback;
    }

    public static Result run(Path directory, List<String> command) {
        return run(directory, command, DEFAULT_TIMEOUT);
    }

    public static Result run(Path directory, List<String> command, Duration timeout) {
        Path log = null;
        Process process = null;
        try {
            log = Files.createTempFile("mtool-build", ".log");
            process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();

            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                return new Result(Outcome.TIMEOUT, "timed out after " + timeout.toSeconds() + " seconds");
            }
            return process.exitValue() == 0
                    ? new Result(Outcome.SUCCESS, "")
                    : new Result(Outcome.FAILURE, extractErrors(Files.readString(log)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            return new Result(Outcome.INTERRUPTED, "interrupted");
        } catch (IOException e) {
            return new Result(Outcome.NOT_RUNNABLE, e.getMessage());
        } finally {
            if (log != null) {
                try {
                    Files.deleteIfExists(log);
                } catch (IOException e) {
                    logger.debugf("Could not delete %s: %s", log, e.getMessage());
                }
            }
        }
    }

    /**
     * Keeps the first Maven {@code [ERROR]} lines, or the Gradle "What went wrong" section and compiler
     * errors. Falls back to the end of the output, where build tools report the failure.
     */
    static String extractErrors(String output) {
        List<String> errors = new ArrayList<>();
        String[] lines = output.split("\\R");
        for (int i = 0; i < lines.length && errors.size() < MAX_ERROR_LINES; i++) {
            String line = lines[i];
            if (line.contains("[ERROR]")) {
                String message = line.replaceFirst(".*\\[ERROR]\\s*", "").trim();
                if (!message.isEmpty() && !message.startsWith("BUILD FAILURE") && !message.startsWith("->")
                        && !message.startsWith("To see the full stack trace")) {
                    errors.add(message);
                }
            } else if (line.contains("What went wrong:") && i + 1 < lines.length) {
                errors.add(lines[++i].trim());
            } else if (line.matches(".*\\.(java|kt|groovy):\\d+: error: .*")) {
                errors.add(line.trim());
            }
        }
        if (!errors.isEmpty()) {
            return String.join(" | ", errors);
        }
        String trimmed = output.strip();
        return trimmed.length() > MAX_FALLBACK_CHARS
                ? "..." + trimmed.substring(trimmed.length() - MAX_FALLBACK_CHARS)
                : trimmed;
    }
}
