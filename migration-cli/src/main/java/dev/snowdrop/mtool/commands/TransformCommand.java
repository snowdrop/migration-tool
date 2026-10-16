package dev.snowdrop.mtool.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.transform.TransformationService;
import dev.snowdrop.mtool.model.transform.MigrationTasksExport;
import dev.snowdrop.mtool.transform.provider.impl.OpenRewriteProvider;
import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;

@CommandDefinition(name = "transform", description = "Transform a java application")
public class TransformCommand implements Command<CommandInvocation> {
    private static final Logger logger = Logger.getLogger(TransformCommand.class);

    @Argument(description = "Path to the Java project to transform", required = true)
    @ConfigProperty(name = "analyzer.app.path", defaultValue = "./applications/spring-boot-todo-app")
    public String appPath;

    @Option(shortName = 'v', name = "verbose", hasValue = false, description = "Enable verbose output")
    private boolean verbose;

    @Option(shortName = 'd', name = "dry-run", hasValue = false, description = "Execute OpenRewrite in dry-run mode (preview changes without applying them)")
    private boolean dryRun;

    @Option(shortName = 'p', name = "provider", description = "Migration provider to use (openrewrite, manual). Default: from migration.provider property")
    @ConfigProperty(name = "migration.provider")
    private String provider;

    @ConfigProperty(name = "migration.provider.openrewrite.composite-recipe-name")
    private String compositeRecipeName;

    @ConfigProperty(name = "openrewrite.maven-plugin.version")
    private String openRewriteMavenPluginVersion;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        Path path = resolvePath(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            return CommandResult.FAILURE;
        }
        try {
            startTransformation();
        } catch (Exception e) {
            logger.errorf("Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
        return CommandResult.SUCCESS;
    }

    private Map<String, MigrationTask> loadLatestAnalysisReport(Path projectPath) {
        try {
            Optional<Path> latestJsonFile = findLatestAnalysingReportJson(projectPath);

            if (latestJsonFile.isEmpty()) {
                logger.warnf("No analysing report JSON files found in: %s", projectPath);
                return Map.of();
            }

            Path jsonFile = latestJsonFile.get();
            logger.infof("Loading migration tasks from: %s", jsonFile.getFileName());

            ObjectMapper objectMapper = new ObjectMapper();
            MigrationTasksExport export = objectMapper.readValue(jsonFile.toFile(), MigrationTasksExport.class);

            LinkedHashMap<String, MigrationTask> sortedMigrationTasks = export.migrationTasks().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(task -> task.getRule().order())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                            LinkedHashMap::new));

            return sortedMigrationTasks;

        } catch (IOException e) {
            logger.errorf("❌ Failed to load migration tasks: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return Map.of();
        }
    }

    private Optional<Path> findLatestAnalysingReportJson(Path projectPath) {
        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:analysing-*-report_*.json");

        try (Stream<Path> stream = Files.list(projectPath)) {
            return stream.filter(Files::isRegularFile).filter(path -> matcher.matches(path.getFileName()))
                    .max((path1, path2) -> {
                        try {
                            return Files.getLastModifiedTime(path1).compareTo(Files.getLastModifiedTime(path2));
                        } catch (IOException e) {
                            return path1.getFileName().toString().compareTo(path2.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            logger.errorf("Error searching for JSON files: %s", e.getMessage());
            return Optional.empty();
        }
    }

    private void startTransformation() {
        Instant start = Instant.now();

        Path projectPath = resolvePath(appPath);
        logger.infof("Starting transformation for project at: %s", projectPath);
        logger.infof("Using provider: %s", provider);

        Map<String, MigrationTask> migrationTasks = loadLatestAnalysisReport(projectPath);

        if (migrationTasks.isEmpty()) {
            logger.warn(
                    "No migration tasks found. Please run the analyze command first to generate a migration report.");
            return;
        }

        logger.infof("Found %d migration tasks to process", migrationTasks.size());

        ExecutionContext context = new ExecutionContext(projectPath, verbose, dryRun, provider,
                openRewriteMavenPluginVersion, compositeRecipeName);

        if ("openrewrite".equals(provider)) {
            List<MigrationTask> openrewriteTasks = migrationTasks.values().stream()
                    .filter(task -> task.getRule().instructions() != null
                            && task.getRule().instructions().openrewrite() != null)
                    .toList();

            if (openrewriteTasks.isEmpty()) {
                logger.warn("No tasks with OpenRewrite instructions found");
            } else {
                logger.infof("Batching %d OpenRewrite tasks into a single execution", openrewriteTasks.size());
                OpenRewriteProvider openRewriteProvider = new OpenRewriteProvider();
                ExecutionResult result = openRewriteProvider.executeBatch(openrewriteTasks, context);
                TransformationService ts = new TransformationService();
                ts.logExecutionResult(result, verbose);
            }
        } else {
            for (Map.Entry<String, MigrationTask> entry : migrationTasks.entrySet()) {
                String taskId = entry.getKey();
                MigrationTask task = entry.getValue();
                executeTaskWithProvider(taskId, task, context);
            }
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logger.info("----------------------------------------");
        logger.info("--- Elapsed time: " + timeElapsed + " ms ---");
        logger.info("----------------------------------------");
    }

    private void executeTaskWithProvider(String taskId, MigrationTask task, ExecutionContext context) {
        logger.infof("Processing migration task: %s", taskId);
        if (verbose) {
            logger.infof("   Description: %s", task.getRule().description());
            logger.infof("   Category: %s", task.getRule().category());
            logger.infof("   Effort: %d", task.getRule().effort());
        }

        var instructions = task.getRule().instructions();
        boolean hasInstructions = instructions != null && switch (provider) {
            case "openrewrite" -> instructions.openrewrite() != null;
            case "manual" -> instructions.manual() != null;
            default -> false;
        };

        if (!hasInstructions) {
            logger.warnf("No %s instructions found for task, skipping", provider);
            return;
        }

        TransformationService ts = new TransformationService();
        ExecutionResult result = ts.execute(task, context);
        ts.logExecutionResult(result, verbose);
    }
}
