package dev.snowdrop.commands;

import dev.snowdrop.analyze.JdtLsFactory;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.ResultsService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.snowdrop.analyze.services.LsSearchService.analyzeCodeFromRule;
import static dev.snowdrop.analyze.services.LsSearchService.displayResultsTable;
import static dev.snowdrop.analyze.utils.YamlRuleParser.filterRules;
import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFolder;

@CommandLine.Command(
    name = "analyze",
    description = "Analyze a project for migration"
)
@ApplicationScoped
public class AnalyzeCommand implements Runnable {
    private static final Logger logger = Logger.getLogger(AnalyzeCommand.class);

    @CommandLine.Parameters(
        index = "0",
        description = "Path to the Java project to analyze"
    )
    public String appPath;

    @CommandLine.Option(
        names = {"-r", "--rules"},
        description = "Path to rules directory (default: from config)"
    )
    public String rulesPath;

    @CommandLine.Option(
        names = {"--jdt-ls-path"},
        description = "Path to JDT-LS installation (default: from config)",
        required = false
    )
    public String jdtLsPath;

    @CommandLine.Option(
        names = {"--jdt-workspace"},
        description = "Path to JDT workspace directory (default: from config)",
        required = false
    )
    public String jdtWorkspace;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output"
    )
    private boolean verbose;

    @CommandLine.Option(
        names = {"-o","--output"},
        description = "Export the analysing result using the format. Values: json"
    )
    private String output;

    @CommandLine.Option(
        names = {"-s","--source"},
        description = "Source technology to consider for analysis"
    )
    public String source;

    @CommandLine.Option(
        names = {"-t","--target"},
        description = "Target technology to consider for analysis"
    )
    public String target;

    @Override
    public void run() {
        Path path = Paths.get(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            return;
        }

        try {
            JdtLsFactory jdtLsFactory = new JdtLsFactory();
            jdtLsFactory.initProperties(this);
            jdtLsFactory.launchLsProcess();
            jdtLsFactory.createLaunchLsClient();
            jdtLsFactory.initLanguageServer();

            startAnalyse(jdtLsFactory);
        } catch (Exception e) {
            logger.errorf("❌ Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    private void startAnalyse(JdtLsFactory factory) throws Exception {
        logger.infof("\n🚀 Starting analysis...");

        try {
            List<Rule> rules = parseRulesFromFolder(factory.rulesPath);

            // Filter the rules according to the source and target technology
            List<Rule> filteredRules = filterRules(rules,factory.sourceTechnology,factory.targetTechnology);
            if (filteredRules.isEmpty()) {
                logger.warnf("No rules found !!");
            } else {
                Map<String, MigrationTask> analyzeReport = analyzeCodeFromRule(factory, filteredRules);

                if (!analyzeReport.isEmpty()) {
                    ResultsService.showCsvTable(analyzeReport, factory.sourceTechnology, factory.targetTechnology);
                }

                // Export rules, results and migration instructions as JSON if requested
                if (output != null && output.equals("json")) {
                    exportAsJson(analyzeReport);
                }

                logger.infof("⏳ Waiting for commands to complete...");
                Thread.sleep(5000);
            }

        } finally {
            if (factory.process != null && factory.process.isAlive()) {
                logger.infof("🛑 Shutting down JDT Language Server...");
                factory.process.destroyForcibly();
            }
        }
    }

    private void exportAsJson(Map<String, MigrationTask> analyzeReport) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm").withLocale(Locale.getDefault());
            String dateTimeformated = LocalDateTime.now().format(formatter);

            MigrationTasksExport exportData = new MigrationTasksExport(
                "Migration Analysis Results",
                appPath,
                dateTimeformated,
                analyzeReport
            );

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            File outputFile = new File(String.format("%s/%s_%s.json", appPath, "analysing-report",dateTimeformated));

            // Ensure parent directory exists
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, exportData);
            logger.infof("📄 Migration tasks exported to: %s", outputFile);

        } catch (IOException e) {
            logger.errorf("❌ Failed to export migration tasks to JSON: %s", e.getMessage());
            if (verbose) {
                logger.error("Export error details:", e);
            }
        }
    }

    // Data structure for JSON export
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MigrationTasksExport(
        String title,
        String projectPath,
        String timestamp,
        Map<String, MigrationTask> migrationTasks
    ) {}
}