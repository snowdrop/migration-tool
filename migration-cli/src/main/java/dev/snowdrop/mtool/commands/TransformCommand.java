package dev.snowdrop.mtool.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.transform.TransformationService;
import dev.snowdrop.mtool.model.transform.MigrationTasksExport;
import dev.snowdrop.mtool.transform.provider.ai.Assistant;
import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;

@CommandLine.Command(name = "transform", description = "Transform a java application")
public class TransformCommand implements Runnable {
	private static final Logger logger = Logger.getLogger(TransformCommand.class);

	@CommandLine.Parameters(index = "0", description = "Path to the Java project to transform")
	@ConfigProperty(name = "analyzer.app.path", defaultValue = "./applications/spring-boot-todo-app")
	public String appPath;

	@CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
	private boolean verbose;

	@CommandLine.Option(names = {"-d",
			"--dry-run"}, description = "Execute OpenRewrite in dry-run mode (preview changes without applying them)")
	private boolean dryRun;

	@CommandLine.Option(names = {"-p",
			"--provider"}, description = "Migration provider to use (ai, openrewrite, manual). Default: from migration.provider property")
	@ConfigProperty(name = "migration.provider")
	private String provider;

	@ConfigProperty(name = "migration.provider.openrewrite.composite-recipe-name")
	private String compositeRecipeName;

	@ConfigProperty(name = "openrewrite.maven-plugin.version")
	private String openRewriteMavenPluginVersion;

	@Inject
	Assistant aiAssistant;

	@Override
	public void run() {
		Path path = resolvePath(appPath);
		if (!path.toFile().exists()) {
			logger.errorf("❌ Project path of the application does not exist: %s", appPath);
			return;
		}
		try {
			startTransformation();
		} catch (Exception e) {
			logger.errorf("❌ Error: %s", e.getMessage());
			if (verbose) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Finds the latest analysed report JSON file in the project path and load it
	 *
	 * @param projectPath
	 *            The path to the project directory
	 *
	 * @return Map of migration tasks from the latest JSON file, or empty map if no file found
	 */
	private Map<String, MigrationTask> loadLatestAnalysisReport(Path projectPath) {
		try {
			Optional<Path> latestJsonFile = findLatestAnalysingReportJson(projectPath);

			if (latestJsonFile.isEmpty()) {
				logger.warnf("No analysing report JSON files found in: %s", projectPath);
				return Map.of();
			}

			Path jsonFile = latestJsonFile.get();
			logger.infof("📄 Loading migration tasks from: %s", jsonFile.getFileName());

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

	/**
	 * Finds the latest analysing report JSON file based on timestamp in filename
	 *
	 * @param projectPath
	 *            The path to search for JSON files
	 *
	 * @return Optional containing the latest JSON file path, or empty if none found
	 */
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

	/**
	 * Execute the transformation using the provider
	 */
	private void startTransformation() {
		Instant start = Instant.now();

		Path projectPath = resolvePath(appPath);
		logger.infof("✅ Starting transformation for project at: %s", projectPath);
		logger.infof("🔧 Using provider: %s", provider);

		Map<String, MigrationTask> migrationTasks = loadLatestAnalysisReport(projectPath);

		if (migrationTasks.isEmpty()) {
			logger.warn(
					"❌ No migration tasks found. Please run the analyze command first to generate a migration report.");
			return;
		}

		logger.infof("📋 Found %d migration tasks to process", migrationTasks.size());

		// Configure the Context with the information used by the Provider
		ExecutionContext context = new ExecutionContext(projectPath, verbose, dryRun, provider, aiAssistant,
				openRewriteMavenPluginVersion, compositeRecipeName);

		// Iterate over the list of the migration and tasks
		for (Map.Entry<String, MigrationTask> entry : migrationTasks.entrySet()) {
			String taskId = entry.getKey();
			MigrationTask task = entry.getValue();

			// Execute the task against the provider
			executeTaskWithProvider(taskId, task, context);
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("----------------------------------------");
		logger.info("--- Elapsed time: " + timeElapsed + " ms ---");
		logger.info("----------------------------------------");
	}

	/**
	 * Execute a specific migration task with the configured provider
	 */
	private void executeTaskWithProvider(String taskId, MigrationTask task, ExecutionContext context) {
		logger.infof("🔄 Processing migration task: %s", taskId);
		if (verbose) {
			logger.infof("   Description: %s", task.getRule().description());
			logger.infof("   Category: %s", task.getRule().category());
			logger.infof("   Effort: %d", task.getRule().effort());
		}

		// Check if the task has instructions for the selected provider
		var instructions = task.getRule().instructions();
		boolean hasInstructions = instructions != null && switch (provider) {
			case "openrewrite" -> instructions.openrewrite() != null;
			case "ai" -> instructions.ai() != null;
			case "manual" -> instructions.manual() != null;
			default -> false;
		};

		if (!hasInstructions) {
			logger.warnf("   ⚠️  No %s instructions found for task, skipping", provider);
			return;
		}

		TransformationService ts = new TransformationService();
		ExecutionResult result = ts.execute(task, context);
		ts.logExecutionResult(result, verbose);
	}
}
