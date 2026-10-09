package dev.snowdrop.mtool.model.validate;

import java.util.ArrayList;
import java.util.List;

public class ValidationReport {

    private final String validatorName;
    private final List<ValidationResult> results = new ArrayList<>();

    public ValidationReport(String validatorName) {
        this.validatorName = validatorName;
    }

    public void add(ValidationResult result) {
        results.add(result);
    }

    public String getValidatorName() {
        return validatorName;
    }

    public List<ValidationResult> getResults() {
        return results;
    }

    public long totalRules() {
        return results.size();
    }

    public long passed() {
        return results.stream().filter(r -> r.status() == ValidationResult.Status.PASSED).count();
    }

    public long failed() {
        return results.stream().filter(r -> r.status() == ValidationResult.Status.FAILED).count();
    }

    public long warnings() {
        return results.stream().filter(r -> r.status() == ValidationResult.Status.WARNING).count();
    }

    public String overallStatus() {
        if (failed() == 0) {
            return "PASS";
        } else if (passed() >= failed()) {
            return "PARTIAL";
        } else {
            return "FAIL";
        }
    }
}