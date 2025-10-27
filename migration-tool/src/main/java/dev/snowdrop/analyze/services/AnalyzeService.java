package dev.snowdrop.analyze.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.JdtLsClient;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static dev.snowdrop.analyze.utils.YamlRuleParser.filterRules;
import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFolder;

public class AnalyzeService {

    private final Config config;

    public AnalyzeService(Config config, ScannerFactory scannerFactory) {
        this.config = config;
        this.scannerFactory = scannerFactory;
    }

    private final ScannerFactory scannerFactory;


    public Map<String, MigrationTask> analyzeCodeFromRule(String scannerType, List<Rule> rules) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        CodeScanner scanner = scannerFactory.createScanner(ScannerFactory.Scanner.fromLabel(scannerType), config);
        return scanner.analyze(rules);
    }

}
