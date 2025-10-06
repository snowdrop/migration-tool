package dev.snowdrop.transform.provider.model;

import java.util.List;

/**
 * Result of a provider execution with success status and details.
 */
public record ExecutionResult(
    boolean success,
    String message,
    List<String> details,
    Exception exception
) {

    /**
     * Creates a successful execution result.
     */
    public static ExecutionResult success(String message) {
        return new ExecutionResult(true, message, List.of(), null);
    }

    /**
     * Creates a successful execution result with details.
     */
    public static ExecutionResult success(String message, List<String> details) {
        return new ExecutionResult(true, message, details, null);
    }

    /**
     * Creates a failed execution result.
     */
    public static ExecutionResult failure(String message) {
        return new ExecutionResult(false, message, List.of(), null);
    }

    /**
     * Creates a failed execution result with exception.
     */
    public static ExecutionResult failure(String message, Exception exception) {
        return new ExecutionResult(false, message, List.of(), exception);
    }

    /**
     * Creates a failed execution result with details and exception.
     */
    public static ExecutionResult failure(String message, List<String> details, Exception exception) {
        return new ExecutionResult(false, message, details, exception);
    }
}