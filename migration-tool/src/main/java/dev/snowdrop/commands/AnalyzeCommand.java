package dev.snowdrop.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.analyze.JdtLsClient;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.AnalyzeService;
import dev.snowdrop.analyze.services.ResultsService;
import dev.snowdrop.analyze.services.ScannerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dev.snowdrop.analyze.utils.FileUtils.resolvePath;
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
        names = {"-s","--source"},
        description = "Source technology to consider for analysis"
    )
    public String source;

    @CommandLine.Option(
        names = {"-t","--target"},
        description = "Target technology to consider for analysis"
    )
    public String target;

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
        names = {"--scanner"},
        description = "Scanner tool to be used to analyse the code: jdtls, openrewrite",
        defaultValue = "jdtls"
    )
    public String scanner;

    @Override
    public void run() {
        Config config = fromCommandOrElseProperties();
        try{
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
                .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-source", String.class)))
                .orElseThrow(() -> new RuntimeException("Source technology to analyse required but not configured"));

        String targetTechnology = Optional.ofNullable(target)
                .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-target", String.class)))
                .orElseThrow(() -> new RuntimeException("Target technology for migration is requiered but not configured"));

        String jdtLsPathString = Optional.ofNullable(jdtLsPath)
                .or(()->Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-path", String.class)))
                .orElseThrow(() -> new RuntimeException("JDT LS path is required but not configured"));
        jdtLsPathString = resolvePath(jdtLsPathString).toString();

        String jdtWksString = Optional.ofNullable(jdtWorkspace)
                .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-workspace-path", String.class)))
                .orElseThrow(() -> new RuntimeException("Jdt workspace is required but not configured"));
        jdtWksString = resolvePath(jdtWksString).toString();

        String lsCmd = Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-command", String.class))
                .orElseThrow(() -> new RuntimeException("Command to be executed against the LS server is required but not configured"));

        Config config = new Config(appPathString,rulesPath,sourceTechnology, targetTechnology, jdtLsPathString, jdtWksString, lsCmd,verbose,output,scanner);



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
        List<Rule> filteredRules = filterRules(rules,sourceTech,targetTech);
        if (filteredRules.isEmpty()) {
            logger.warnf("No rules found !!");
        }
        return filteredRules;
    }

    public void displayResults(Map<String, MigrationTask> tasks, Config config) throws InterruptedException {
        if (!tasks.isEmpty()) {
            ResultsService.showCsvTable(tasks, config.sourceTechnology(), config.targetTechnology());
        }

        // Export rules, results and migration instructions as JSON if requested
        if (config.output() != null && config.output().equals("json")) {
            exportAsJson(tasks, config);
        }

        logger.infof("⏳ Waiting for commands to complete...");
        Thread.sleep(5000);
    }

    private void exportAsJson(Map<String, MigrationTask> analyzeReport, Config config) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm").withLocale(Locale.getDefault());
            String dateTimeformated = LocalDateTime.now().format(formatter);

            MigrationTasksExport exportData = new MigrationTasksExport(
                    "Migration Analysis Results",
                    config.appPath(),
                    dateTimeformated,
                    analyzeReport
            );

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            File outputFile = new File(String.format("%s/analysing-%s-report_%s.json", config.appPath(),config.scanner(),dateTimeformated));

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
    public record MigrationTasksExport(
            String title,
            String projectPath,
            String timestamp,
            Map<String, MigrationTask> migrationTasks
    ) {}


}