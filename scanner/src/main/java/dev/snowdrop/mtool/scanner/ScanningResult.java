package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.analyze.Result;

import java.util.List;
import java.util.Map;

public class ScanningResult {
    private boolean matchSucceeded = false;
    private final Map<String, List<Result>> results;

    public ScanningResult(boolean matchSucceeded, Map<String, List<Result>> results) {
        this.matchSucceeded = matchSucceeded;
        this.results = results;
    }

    public ScanningResult(Map<String, List<Result>> results) {
        this.results = results;
    }

    public boolean isMatchSucceeded() {
        return matchSucceeded;
    }

    public Map<String, List<Result>> getResults() {
        return results;
    }
}
