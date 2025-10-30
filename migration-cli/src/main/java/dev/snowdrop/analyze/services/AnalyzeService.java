package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class AnalyzeService {

    private final Config config;

    public AnalyzeService(Config config, ScannerFactory scannerFactory) {
        this.config = config;
        this.scannerFactory = scannerFactory;
    }

    private final ScannerFactory scannerFactory;

    public Map<String, MigrationTask> analyzeCodeFromRule(String scannerType, List<Rule> rules)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        CodeScanner scanner = scannerFactory.createScanner(ScannerFactory.Scanner.fromLabel(scannerType), config);
        return scanner.analyze(rules);
    }

}
