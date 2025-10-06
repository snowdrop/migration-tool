package dev.snowdrop.transform.provider.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.transform.model.CompositeRecipe;
import dev.snowdrop.transform.provider.MigrationProvider;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider implementation for executing OpenRewrite transformations via Maven plugin.
 */
public class OpenRewriteProvider implements MigrationProvider {

    private static final Logger logger = Logger.getLogger(OpenRewriteProvider.class);

    public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
    public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";

    private static final String COMPOSITE_RECIPE_NAME = "dev.snowdrop.openrewrite.java.SpringToQuarkus";

    @Override
    public String getProviderType() {
        return "openrewrite";
    }

    @Override
    public ExecutionResult execute(MigrationTask task, ExecutionContext context) {
        var rule = task.getRule();

        if (rule.instructions() == null || rule.instructions().openrewrite() == null) {
            return ExecutionResult.failure("No OpenRewrite instructions found for task");
        }

        List<String> executionDetails = new ArrayList<>();
        boolean allSuccessful = true;

        for (var openrewrite : rule.instructions().openrewrite()) {
            if (openrewrite.recipeList() == null || openrewrite.recipeList().isEmpty()) {
                logger.warnf("No recipes defined in OpenRewrite instruction, skipping");
                executionDetails.add("Skipped instruction with no recipes");
                continue;
            }

            try {
                var result = executeOpenRewriteInstruction(openrewrite, rule, context);
                executionDetails.addAll(result.details());

                if (!result.success()) {
                    allSuccessful = false;
                    executionDetails.add("Failed: " + result.message());
                }
            } catch (Exception e) {
                allSuccessful = false;
                executionDetails.add("Exception during execution: " + e.getMessage());
                logger.errorf("Error executing OpenRewrite instruction: %s", e.getMessage());
                if (context.verbose()) {
                    e.printStackTrace();
                }
            }
        }

        if (allSuccessful) {
            return ExecutionResult.success("OpenRewrite execution completed successfully", executionDetails);
        } else {
            return ExecutionResult.failure("Some OpenRewrite executions failed", executionDetails, null);
        }
    }

    private ExecutionResult executeOpenRewriteInstruction(Rule.Openrewrite openrewrite, Rule rule, ExecutionContext context) {
        List<String> details = new ArrayList<>();

        // Generate YAML recipes
        String yamlRecipesStr;
        try {
            yamlRecipesStr = populateYAMLRecipes(openrewrite);
            details.add("Generated YAML recipes");
        } catch (Exception e) {
            return ExecutionResult.failure("Failed to generate YAML recipes", e);
        }

        // Write YAML file
        String rewriteYamlName = String.format("rewrite-%d.yml", rule.order());
        Path yamlFilePath = context.projectPath().resolve(rewriteYamlName);

        try {
            Files.write(yamlFilePath, yamlRecipesStr.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            details.add("Created YAML file: " + rewriteYamlName);
        } catch (IOException e) {
            return ExecutionResult.failure("Failed to write YAML file", e);
        }

        // Execute Maven command
        String gavs = String.join(",", openrewrite.gav());
        boolean success = execMvnCmd(context, gavs, rewriteYamlName, details);

        if (success) {
            return ExecutionResult.success("OpenRewrite instruction executed successfully", details);
        } else {
            return ExecutionResult.failure("Maven command execution failed", details, null);
        }
    }

    private String populateYAMLRecipes(Rule.Openrewrite openrewrite) throws JsonProcessingException {
        List<Object> recipes = openrewrite.recipeList();

        if (recipes.isEmpty()) {
            throw new IllegalStateException("No recipes defined in OpenRewrite instruction");
        }

        CompositeRecipe compositeRecipe = new CompositeRecipe(
            COMPOSITE_RECIPE_NAME,
            openrewrite.name(),
            openrewrite.description(),
            recipes
        );

        ObjectMapper mapper = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        );

        return mapper.writeValueAsString(compositeRecipe);
    }

    private boolean execMvnCmd(ExecutionContext context, String gavs, String rewriteYamlName, List<String> details) {
        try {
            List<String> command = new ArrayList<>();
            String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

            command.add("mvn");
            command.add("-B");
            command.add("-e");
            // Note: Version should be injected from configuration
            String pluginVersion = "6.19.0"; // Default fallback
            command.add(String.format("%s:%s:%s:%s",
                MAVEN_OPENREWRITE_PLUGIN_GROUP,
                MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
                pluginVersion,
                context.dryRun() ? "dryRun" : "run"));
            command.add("-Drewrite.activeRecipes=" + COMPOSITE_RECIPE_NAME);
            command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
            command.add("-Drewrite.exportDatatables=true");
            command.add(String.format("-DreportOutputDirectory=target/%s", outputDirectoryRewriteName));
            command.add(String.format("-Drewrite.configLocation=%s", rewriteYamlName));

            String commandStr = String.join(" ", command);
            logger.infof("Executing command: %s", commandStr);
            details.add("Executing: " + commandStr);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(context.projectPath().toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read and log output in real-time
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (context.verbose()) {
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
            if (context.verbose()) {
                e.printStackTrace();
            }
            return false;
        }
    }
}