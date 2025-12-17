package dev.snowdrop.mtool.transform.provider.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.model.transform.CompositeRecipe;
import dev.snowdrop.mtool.transform.provider.MigrationProvider;
import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider implementation for executing OpenRewrite transformations via Maven plugin.
 */
public class OpenRewriteProvider implements MigrationProvider {

	private static final Logger logger = Logger.getLogger(OpenRewriteProvider.class);

	public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
	public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";

	@Override
	public String getProviderType() {
		return "openrewrite";
	}

	@Override
	public ExecutionResult execute(MigrationTask task, ExecutionContext ctx) {
		var rule = task.getRule();

		if (rule.instructions() == null || rule.instructions().openrewrite() == null) {
			return ExecutionResult
					.failure(String.format("No OpenRewrite instructions found for the rule: %s", rule.ruleID()));
		}

		// We only process until now only one openrewrite object/rule !!
		var openrewrite = Arrays.stream(rule.instructions().openrewrite()).findFirst().orElse(null);

		if (openrewrite.recipeList() == null || openrewrite.recipeList().isEmpty()) {
			return ExecutionResult.failure("No recipes defined in OpenRewrite instruction, skipping", null);
		}

		try {
			ExecutionResult result = executeOpenRewriteInstruction(ctx, openrewrite, rule);

			if (!result.success()) {
				return ExecutionResult.failure(result.message(), result.details(), null);
			}

			return ExecutionResult.success("OpenRewrite execution completed successfully", result.details());
		} catch (Exception e) {
			logger.errorf("Error executing OpenRewrite instruction: %s", e.getMessage());
			if (ctx.verbose()) {
				e.printStackTrace();
			}
			return ExecutionResult.failure("Error executing OpenRewrite instruction !", e);
		}
	}

	private ExecutionResult executeOpenRewriteInstruction(ExecutionContext ctx, Rule.Openrewrite openrewrite,
			Rule rule) {
		List<String> details = new ArrayList<>();

		// Generate YAML recipes
		String yamlRecipesStr;
		try {
			yamlRecipesStr = populateYAMLRecipes(ctx, openrewrite);
			details.add("Generated YAML recipes");
		} catch (Exception e) {
			return ExecutionResult.failure("Failed to generate YAML recipes", e);
		}

		// Write YAML file
		String rewriteYamlName = String.format("rewrite-%d.yml", rule.order());
		Path yamlFilePath = ctx.projectPath().resolve(rewriteYamlName);

		try {
			Files.write(yamlFilePath, yamlRecipesStr.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			details.add("Created YAML file: " + rewriteYamlName);
		} catch (IOException e) {
			return ExecutionResult.failure("Failed to write YAML file", e);
		}

		// Execute Maven command
		String gavs = String.join(",", openrewrite.gav());
		boolean success = execMvnCmd(ctx, gavs, rewriteYamlName, details);
		details.add("Maven cmd executed successfully");

		if (success) {
			return ExecutionResult.success("OpenRewrite instruction executed successfully", details);
		} else {
			return ExecutionResult.failure("Openrewrite's maven command execution failed", details, null);
		}
	}

	private String populateYAMLRecipes(ExecutionContext ctx, Rule.Openrewrite openrewrite)
			throws JsonProcessingException {
		List<Object> recipes = openrewrite.recipeList();

		if (recipes.isEmpty()) {
			throw new IllegalStateException("No recipes defined in OpenRewrite instruction");
		}

		CompositeRecipe compositeRecipe = new CompositeRecipe(ctx.compositeRecipeName(), openrewrite.name(),
				openrewrite.description(), recipes);

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

		return mapper.writeValueAsString(compositeRecipe);
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