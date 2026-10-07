package dev.snowdrop.mtool.model.validate;

public record ValidationResult(ValidationRule rule, Status status, String evidence) {

    public enum Status {
        PASSED,
        FAILED,
        WARNING
    }
}