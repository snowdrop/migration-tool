package dev.snowdrop.mtool.commands;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.mtool.analyze.services.ResultsService;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Plan;
import dev.snowdrop.mtool.model.analyze.Result;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.parser.QueryParserUtil;
import dev.snowdrop.mtool.scanner.CodeScannerService;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import dev.snowdrop.mtool.scanner.ScanningResult;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;

@CommandLine.Command(name = "scan", description = "Scan an application against a query or a plan")
public class ScannerCommand implements Runnable {
    private static final Logger logger = Logger.getLogger(ScannerCommand.class);

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @CommandLine.Parameters(index = "0", description = "Path to the Java project to analyze")
    public String appPath;

    @CommandLine.Parameters(index = "1", description = "Query to be executed", defaultValue = "")
    public String query;

    @CommandLine.Option(names = {
            "--scanner" }, description = "Scanner tool to be used to analyse the code: jdtls, openrewrite, file, maven, treesitter", defaultValue = "openrewrite")
    public String scanner;

    @CommandLine.Option(names = {
            "--plan" }, description = "Path to a YAML plan file containing queries to execute")
    public String planFile;

    @CommandLine.Option(names = { "-o",
            "--output" }, description = "Export the scan result using as format: json, csv, html")
    private String output;

    @Override
    public void run() {
        long startTime = System.nanoTime();

        Path path = Paths.get(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("Project path of the application does not exist: %s", appPath);
            throw new IllegalStateException("Project path of the application does not exist: " + appPath);
        }
        String appPathString = resolvePath(appPath).toString();
        Config config = new Config(appPathString, null, null, null, null, null, null, false, null, scanner, null);

        if (planFile != null && !planFile.isBlank()) {
            executePlan(config, startTime);
        } else {
            executeSingleQuery(config, startTime);
        }
    }

    private void executePlan(Config config, long startTime) {
        Path planPath = Paths.get(planFile);
        if (!Files.exists(planPath)) {
            throw new IllegalStateException("Plan file does not exist: " + planFile);
        }

        Plan plan;
        try {
            plan = yamlMapper.readValue(Files.newInputStream(planPath), Plan.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse plan file: " + planFile, e);
        }

        logger.infof("Executing plan '%s' with %d queries", plan.getName(), plan.getQueries().size());

        ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
        CodeScannerService codeScannerService = new CodeScannerService(config, scanCommandExecutor);
        ScanningResult scanningResult = codeScannerService.scan(plan);

        Map<String, List<Result>> results = scanningResult.getResults();
        if (results != null) {
            displayResults(results, config);
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        int totalResults = results != null ? results.values().stream().mapToInt(List::size).sum() : 0;
        logger.infof("%d result(s) from plan '%s'. Elapsed: %d ms", totalResults, plan.getName(), elapsedMs);
    }

    private void executeSingleQuery(Config config, long startTime) {
        Query q;

        if (!query.isBlank()) {
            QueryParserUtil queryParserUtil = new QueryParserUtil();
            Optional<Query> userQuery = queryParserUtil.parseQuery(query).getSimpleQueries().stream().findFirst();
            q = userQuery.get();
            logger.infof("Processing user's query: %s", query);
        } else {
            q = new Query("pom", "dependency", "", Map.of(
                    "gavs", "org.springframework.boot:spring-boot-starter-parent:3.5.3"));
        }

        ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
        List<Result> matches = scanCommandExecutor.executeCommandForQuery(config, q);

        Map<String, List<Result>> results = Map.of(q.fileType() + ":" + q.symbol(), matches);
        displayResults(results, config);

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        logger.infof(matches.size() + " match(es). Elapsed: " + elapsedMs + " ms");
    }

    private void displayResults(Map<String, List<Result>> results, Config config) {
        ResultsService resultsService = new ResultsService();

        resultsService.showScanTable(results);

        if (output != null && !output.isEmpty()) {
            switch (output) {
                case "json" -> resultsService.exportScanResultsAsJson(config, results);
                case "html" -> resultsService.exportScanAsHtml(config, results);
                case "csv" -> resultsService.exportScanAsCsv(config, results);
                default -> logger.warnf("The format selected to export the report is unknown: %s", output);
            }
        }
    }
}
