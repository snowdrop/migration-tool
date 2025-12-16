package dev.snowdrop.mtool.scanner;

/**
 * Exception thrown when a rule's precondition is not met during scanning.
 * This indicates that the project does not satisfy the required preconditions
 * for the rule to be applicable.
 */
public class PreconditionFailedException extends RuntimeException {

	public PreconditionFailedException(String message) {
		super(message);
	}

	public PreconditionFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}