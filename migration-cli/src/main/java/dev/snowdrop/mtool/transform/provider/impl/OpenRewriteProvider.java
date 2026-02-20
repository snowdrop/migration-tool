package dev.snowdrop.mtool.transform.provider.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.logging.LoggingService;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.model.openrewrite.CompositeRecipe;
import dev.snowdrop.mtool.transform.provider.MigrationProvider;
import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;
import dev.snowdrop.openrewrite.cli.RewriteService;
import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Provider implementation for executing OpenRewrite transformations via rewrite-client.
 */
public class OpenRewriteProvider implements MigrationProvider {

	private static final Logger logger = Logger.getLogger(OpenRewriteProvider.class);

	public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
	public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";

	private static final String REWRITE_YAML_NAME = "rewrite.yml";

	@Override
	public String getProviderType() {
		return "openrewrite";
	}

	@Override
	public ExecutionResult execute(MigrationTask task, ExecutionContext ctx) {
		return executeBatch(List.of(task), ctx);
	}

	/**
	 * Executes all OpenRewrite tasks in a single invocation by merging their recipes and GAV dependencies.
	 *
	 * @param tasks
	 *            the list of migration tasks containing OpenRewrite instructions
	 * @param ctx
	 *            the execution context
	 *
	 * @return execution result from the merged run
	 */
	public ExecutionResult executeBatch(List<MigrationTask> tasks, ExecutionContext ctx) {
		List<Object> mergedRecipes = new ArrayList<>();
		LinkedHashSet<String> mergedGavs = new LinkedHashSet<>();
		String firstName = null;
		String firstDescription = null;

		for (MigrationTask task : tasks) {
			var rule = task.getRule();

			if (rule.instructions() == null || rule.instructions().openrewrite() == null) {
				logger.warnf("No OpenRewrite instructions found for rule: %s, skipping", rule.ruleID());
				continue;
			}

			for (Rule.Openrewrite openrewrite : rule.instructions().openrewrite()) {
				if (openrewrite.recipeList() == null || openrewrite.recipeList().isEmpty()) {
					continue;
				}

				if (firstName == null) {
					firstName = openrewrite.name();
					firstDescription = openrewrite.description();
				}

				mergedRecipes.addAll(openrewrite.recipeList());

				if (openrewrite.gav() != null) {
					mergedGavs.addAll(Arrays.asList(openrewrite.gav()));
				}
			}
		}

		if (mergedRecipes.isEmpty()) {
			return ExecutionResult.failure("No recipes found across all OpenRewrite tasks");
		}

		try {
			return executeMergedInstruction(ctx, firstName, firstDescription, mergedRecipes,
					new ArrayList<>(mergedGavs));
		} catch (Exception e) {
			logger.errorf("Error executing OpenRewrite instruction: %s", e.getMessage());
			if (ctx.verbose()) {
				e.printStackTrace();
			}
			return ExecutionResult.failure("Error executing OpenRewrite instruction !", e);
		}
	}

	private ExecutionResult executeMergedInstruction(ExecutionContext ctx, String name, String description,
			List<Object> recipes, List<String> gavs) {
		List<String> details = new ArrayList<>();

		// Generate YAML recipes
		String yamlRecipesStr;
		try {
			yamlRecipesStr = populateYAMLRecipes(ctx, name, description, recipes);
			details.add("Generated YAML recipes");
		} catch (Exception e) {
			return ExecutionResult.failure("Failed to generate YAML recipes", e);
		}

		// Write YAML file
		Path yamlFilePath = ctx.projectPath().resolve(REWRITE_YAML_NAME);

		try {
			Files.write(yamlFilePath, yamlRecipesStr.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			details.add("Created YAML file: " + REWRITE_YAML_NAME);
		} catch (IOException e) {
			return ExecutionResult.failure("Failed to write YAML file", e);
		}

		// Execute via rewrite-client
		boolean success = runRewriteService(ctx, REWRITE_YAML_NAME, details, gavs);

		if (success) {
			return ExecutionResult.success("OpenRewrite execution completed successfully", details);
		} else {
			return ExecutionResult.failure("OpenRewrite execution failed", details, null);
		}
	}

	private String populateYAMLRecipes(ExecutionContext ctx, String name, String description, List<Object> recipes)
			throws JsonProcessingException {
		if (recipes.isEmpty()) {
			throw new IllegalStateException("No recipes defined in OpenRewrite instruction");
		}

		CompositeRecipe compositeRecipe = new CompositeRecipe(ctx.compositeRecipeName(), name, description, recipes);

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

		return mapper.writeValueAsString(compositeRecipe);
	}

	private boolean runRewriteService(ExecutionContext ctx, String rewriteYamlName, List<String> details,
			List<String> gavs) {
		try {
			RewriteConfig cfg = new RewriteConfig();
			cfg.setAppPath(ctx.projectPath());
			cfg.setYamlRecipesPath(rewriteYamlName);
			cfg.setDryRun(ctx.dryRun());
			cfg.setAdditionalJarPaths(gavs);

			RewriteService svc = new RewriteService(cfg);
			svc.setLogger(new LoggingService());
			svc.init();

			ResultsContainer results = svc.run();

			details.add(String.format("Files modified: %d", results.getRefactoredInPlace().size()));
			details.add(String.format("Files created: %d", results.getGenerated().size()));
			details.add(String.format("Files deleted: %d", results.getDeleted().size()));
			details.add(String.format("Files moved: %d", results.getMoved().size()));

			if (results.getFirstException() != null) {
				details.add("Exception: " + results.getFirstException().getMessage());
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.errorf("Failed to execute rewrite-client: %s", e.getMessage());
			details.add("Error executing rewrite-client: " + e.getMessage());
			if (ctx.verbose()) {
				e.printStackTrace();
			}
			return false;
		}
	}

	private boolean execMvnCmd(ExecutionContext ctx, String gavs, String rewriteYamlName, List<String> details) {
		try {
			List<String> command = new ArrayList<>();
			String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

			command.add("mvn");
			command.add("-B");
			command.add("-e");
			command.add(String.format("%s:%s:%s:%s", MAVEN_OPENREWRITE_PLUGIN_GROUP, MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
					ctx.openRewriteMavenPluginVersion(), ctx.dryRun() ? "dryRun" : "run"));
			command.add(String.format("-Drewrite.activeRecipes=%s", ctx.compositeRecipeName()));
			command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
			command.add("-Drewrite.exportDatatables=true");
			command.add(String.format("-DreportOutputDirectory=target/%s", outputDirectoryRewriteName));
			command.add(String.format("-Drewrite.configLocation=%s", rewriteYamlName));

			String commandStr = String.join(" ", command);
			logger.infof("Executing command: %s", commandStr);
			details.add("Executing: " + commandStr);

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(ctx.projectPath().toFile());
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

			// Read and log output in real-time
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (ctx.verbose()) {
						logger.infof("      %s", line);
					}
					details.add("Maven: " + line);
				}
			}

			int exitCode = process.waitFor();
			details.add("Maven exit code: " + exitCode);

			return exitCode == 0;

		} catch (IOException | InterruptedException e) {
			logger.errorf("Failed to execute maven command: %s", e.getMessage());
			details.add("Error executing Maven: " + e.getMessage());
			if (ctx.verbose()) {
				e.printStackTrace();
			}
			return false;
		}
	}
}
