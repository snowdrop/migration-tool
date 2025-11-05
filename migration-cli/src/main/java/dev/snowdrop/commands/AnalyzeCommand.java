package dev.snowdrop.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.AnalyzeService;
import dev.snowdrop.analyze.services.ResultsService;
import dev.snowdrop.analyze.services.ScannerFactory;
import io.quarkus.qute.Engine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dev.snowdrop.analyze.services.ResultsService.exportAsCsv;
import static dev.snowdrop.analyze.services.ResultsService.exportAsHtml;
import static dev.snowdrop.analyze.utils.FileUtils.resolvePath;
import static dev.snowdrop.analyze.utils.YamlRuleParser.filterRules;
import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFolder;

@CommandLine.Command(name = "analyze", description = "Analyze a project for migration")
@ApplicationScoped
public class AnalyzeCommand implements Runnable {
    private static final Logger logger = Logger.getLogger(AnalyzeCommand.class);

    @CommandLine.Parameters(index = "0", description = "Path to the Java project to analyze")
    public String appPath;

    @CommandLine.Option(names = { "-r", "--rules" }, description = "Path to rules directory (default: from config)")
    public String rulesPath;

    @CommandLine.Option(names = { "-s", "--source" }, description = "Source technology to consider for analysis")
    public String source;

    @CommandLine.Option(names = { "-t", "--target" }, description = "Target technology to consider for analysis")
    public String target;

    @CommandLine.Option(names = {
            "--jdt-ls-path" }, description = "Path to JDT-LS installation (default: from config)", required = false)
    public String jdtLsPath;

    @CommandLine.Option(names = {
            "--jdt-workspace" }, description = "Path to JDT workspace directory (default: from config)", required = false)
    public String jdtWorkspace;

    @CommandLine.Option(names = { "-v", "--verbose" }, description = "Enable verbose output")
    private boolean verbose;

    @CommandLine.Option(names = { "-o",
            "--output" }, description = "Export the analysing result using as format: json (default), csv, html")
    private String output;

    @CommandLine.Option(names = {
            "--scanner" }, description = "Scanner tool to be used to analyse the code: jdtls, openrewrite", defaultValue = "jdtls")
    public String scanner;

    @Inject
    Engine quteTemplateEngine;

    @Override
    public void run() {
        Config config = fromCommandOrElseProperties();
        try {
            List<Rule> rules = loadRules(config.rulesPath(), config.sourceTechnology(), config.targetTechnology());

            AnalyzeService analyzeService = new AnalyzeService(config, new ScannerFactory());
            Map<String, MigrationTask> tasks = analyzeService.analyzeCodeFromRule(scanner, rules);

            displayResults(tasks, config);

        } catch (Exception e) {
            logger.errorf("❌ Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }

    }

    public Config fromCommandOrElseProperties() {
        Path path = Paths.get(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            throw new IllegalStateException("❌ Project path of the application does not exist: %s\", appPath");
        }

        String appPathString = appPath;
        appPathString = resolvePath(appPathString).toString();

        String rulesPathString = Optional.ofNullable(rulesPath)
                .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.rules-path", String.class)))
                .orElseThrow(() -> new RuntimeException("Rules path is required but not configured"));
        Path rulesPath = resolvePath(rulesPathString);

        String sourceTechnology = Optional.ofNullable(source)
                .or(() -> Optional
                        .ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-source", String.class)))
                .orElseThrow(() -> new RuntimeException("Source technology to analyse required but not configured"));

        String targetTechnology = Optional.ofNullable(target)
                .or(() -> Optional
                        .ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-target", String.class)))
                .orElseThrow(
                        () -> new RuntimeException("Target technology for migration is requiered but not configured"));

        String jdtLsPathString = Optional.ofNullable(jdtLsPath).or(
                () -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-path", String.class)))
                .orElseThrow(() -> new RuntimeException("JDT LS path is required but not configured"));
        jdtLsPathString = resolvePath(jdtLsPathString).toString();

        String jdtWksString = Optional.ofNullable(jdtWorkspace)
                .or(() -> Optional
                        .ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-workspace-path", String.class)))
                .orElseThrow(() -> new RuntimeException("Jdt workspace is required but not configured"));
        jdtWksString = resolvePath(jdtWksString).toString();

        String lsCmd = Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-command", String.class))
                .orElseThrow(() -> new RuntimeException(
                        "Command to be executed against the LS server is required but not configured"));

        Config config = new Config(appPathString, rulesPath, sourceTechnology, targetTechnology, jdtLsPathString,
                jdtWksString, lsCmd, verbose, output, scanner);

        // Log resolved paths for debugging
        logger.infof("📋 Jdt-ls path: %s", jdtLsPath);
        logger.infof("📋 Jdt-ls workspace: %s", jdtWksString);
        logger.infof("📋 Language server command: %s", lsCmd);
        logger.infof("📋 Application path: %s", appPath);
        logger.infof("📋 Source technology: %s", sourceTechnology);
        logger.infof("📋 Target technology: %s", targetTechnology);
        return config;
    }

    public List<Rule> loadRules(Path rulesPath, String sourceTech, String targetTech) throws IOException {

        List<Rule> rules = parseRulesFromFolder(rulesPath);

        // Filter the rules according to the source and target technology
        List<Rule> filteredRules = filterRules(rules, sourceTech, targetTech);
        if (filteredRules.isEmpty()) {
            logger.warnf("No rules found !!");
        }
        return filteredRules;
    }

    public void displayResults(Map<String, MigrationTask> tasks, Config config) throws InterruptedException {
        if (tasks.isEmpty()) {
            logger.warnf("No migration tasks found !!");
        }

        // Render by default the Ascii Table of results
        List<String[]> tableData = generateDataTable(tasks, config.sourceTechnology(), config.targetTechnology());
        ResultsService.showCsvTable(tableData);

        // Export rules, results and migration instructions
        switch (config.output()) {
        case "html":
            exportAsHtml(config, tableData);
            break;
        case "csv":
            exportAsCsv(config, tableData);
            break;
        case "json":
        default:
            exportAsJson(config, tasks);
            break;
        }

        logger.infof("⏳ Waiting for commands to complete...");
        Thread.sleep(2000);
    }

    private static List<String[]> generateDataTable(Map<String, MigrationTask> results, String source, String target) {
        // Prepare data for the table
        List<String[]> tableData = new ArrayList<>();

        for (Map.Entry<String, MigrationTask> entry : results.entrySet()) {
            String ruleId = entry.getKey();
            MigrationTask aTask = entry.getValue();

            List<?> queryResults = new ArrayList<>();

            if (aTask.getLsResults() != null && !aTask.getLsResults().isEmpty()) {
                queryResults = aTask.getLsResults();
            }

            if (aTask.getRewriteResults() != null && !aTask.getRewriteResults().isEmpty()) {
                queryResults = aTask.getRewriteResults();
            }

            String hasQueryResults = queryResults.isEmpty() ? "No" : "Yes";
            String sourceToTarget = String.format("%s -> %s", source, target);

            if (queryResults.isEmpty()) {
                tableData.add(new String[] { ruleId, sourceToTarget, hasQueryResults, "No match found" });
            } else {
                // Process ALL results, not just the first one
                StringBuilder allResultsDetails = new StringBuilder();

                for (int i = 0; i < queryResults.size(); i++) {
                    Object result = queryResults.get(i);

                    if (result instanceof SymbolInformation symbolInfo) {
                        String symbolDetails = formatSymbolInformation(symbolInfo);
                        allResultsDetails.append(symbolDetails).append("\n").append(symbolInfo.getLocation().getUri());
                    } else if (result instanceof Rewrite rewrite) {
                        String rewriteDetails = formatRewriteImproved(rewrite);
                        allResultsDetails.append(rewriteDetails);
                    } else {
                        // Fallback for unknown types
                        allResultsDetails.append("Unknown result type: ").append(result.getClass().getSimpleName());
                    }

                    // Add separator between multiple results (except for the last one)
                    if (i < queryResults.size() - 1) {
                        allResultsDetails.append("\n--- rewrite ---\n");
                    }
                }

                tableData.add(new String[] { ruleId, sourceToTarget, hasQueryResults, allResultsDetails.toString() });
            }
        }

        // Sorts the List<String[]> by comparing the first element (row[0]) of each array which is the RuleID
        tableData.sort(Comparator.comparing(row -> row[0]));

        return tableData;
    }

    private static String formatRewriteImproved(Rewrite rewrite) {
        String name = rewrite.name();

        // Parse the name format: parentFolderName/csvFileName:line_number|pattern.symbol|type
        if (name.contains("/") && name.contains(":") && name.contains("|")) {
            try {
                String[] pathAndRest = name.split(":", 2);
                String path = pathAndRest[0];
                String[] lineAndDetails = pathAndRest[1].split("\\|", 3);
                String lineNumber = lineAndDetails[0];
                String patternSymbol = lineAndDetails.length > 1 ? lineAndDetails[1] : "N/A";
                String type = lineAndDetails.length > 2 ? lineAndDetails[2] : "N/A";

                return String.format("File: %s\nLine: %s\nPattern: %s\nType: %s\nMatch ID: %s", path, lineNumber,
                        patternSymbol, type, rewrite.matchId());
            } catch (Exception e) {
                // Fallback if parsing fails
                return String.format("Rewrite match: %s (ID: %s)", rewrite.name(), rewrite.matchId());
            }
        } else {
            // Fallback for unexpected format
            return String.format("Rewrite match: %s (ID: %s)", rewrite.name(), rewrite.matchId());
        }
    }

    private static String formatSymbolInformation(SymbolInformation si) {
        return String.format("Found %s at line %s, char: %s - %s", si.getName(),
                si.getLocation().getRange().getStart().getLine() + 1,
                si.getLocation().getRange().getStart().getCharacter(),
                si.getLocation().getRange().getEnd().getCharacter());
    }

    private void exportAsJson(Config config, Map<String, MigrationTask> tasks) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                    .withLocale(Locale.getDefault());
            String dateTimeformated = LocalDateTime.now().format(formatter);

            MigrationTasksExport exportData = new MigrationTasksExport("Migration Analysis Results", config.appPath(),
                    dateTimeformated, tasks);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            File outputFile = new File(String.format("%s/analysing-%s-report_%s.json", config.appPath(),
                    config.scanner(), dateTimeformated));

            // Ensure parent directory exists
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, exportData);
            logger.infof("📄 Migration tasks exported to: %s", outputFile);

        } catch (IOException e) {
            logger.errorf("❌ Failed to export migration tasks to JSON: %s", e.getMessage());
            if (config.verbose()) {
                logger.error("Export error details:", e);
            }
        }
    }

    // Data structure for JSON export
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MigrationTasksExport(String title, String projectPath, String timestamp,
            Map<String, MigrationTask> migrationTasks) {
    }

}