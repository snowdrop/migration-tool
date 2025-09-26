package dev.snowdrop.commands;

import dev.snowdrop.analyze.JdtLsFactory;
import dev.snowdrop.analyze.model.MigrationTask;
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
import java.util.Locale;
import java.util.Map;

import static dev.snowdrop.analyze.services.LsSearchService.analyzeCodeFromRule;

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
    @ConfigProperty(name = "analyzer.app.path", defaultValue = "./applications/spring-boot-todo-app")
    public String appPath;

    @CommandLine.Option(
        names = {"-r", "--rules"},
        description = "Path to rules directory (default: from config)"
    )
    @ConfigProperty(name = "analyzer.rules.path", defaultValue = "./rules")
    public String rulesPath;

    @CommandLine.Option(
        names = {"--jdt-ls-path"},
        description = "Path to JDT-LS installation (default: from config)",
        required = false
    )
    @ConfigProperty(name = "analyzer.jdt.ls.path", defaultValue = "./jdt/konveyor-jdtls")
    public String jdtLsPath;

    @CommandLine.Option(
        names = {"--jdt-workspace"},
        description = "Path to JDT workspace directory (default: from config)",
        required = false
    )
    @ConfigProperty(name = "analyzer.jdt.workspace.path", defaultValue = "./jdt")
    public String jdtWorkspace;

    @ConfigProperty(name = "analyzer.jdt.ls.command", defaultValue = "java.project.getAll")
    public String lsCommand;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output"
    )
    private boolean verbose;

    @CommandLine.Option(
        names = {"-o","--output"},
        description = "Export the analysing result as json format"
    )
    private String output;

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
        logger.infof("\n🚀 Starting jdt ls analysis...");

        // Log resolved paths for debugging
        logger.infof("📋 jdt ls path: %s", factory.jdtLsPath);
        logger.infof("📋 workspace: %s", factory.jdtWks);
        logger.infof("📋 project path: %s", factory.appPath);
        logger.infof("📋 rules path: %s", factory.rulesPath);
        logger.infof("📋 LS_CMD set to: %s", factory.lsCmd);

        try {
            Map<String, MigrationTask> analyzeReport = analyzeCodeFromRule(factory);

            // Export migration tasks as JSON if requested
            if (output != null && output.equals("json")) {
                exportAsJson(analyzeReport);
            }

            logger.infof("⏳ Waiting for commands to complete...");
            Thread.sleep(5000);

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