package dev.snowdrop.analyze.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.mapper.QueryToRecipeMapper;
import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeDTOSerializer;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import dev.snowdrop.transform.model.CompositeRecipe;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class RewriteService {
    private static final Logger logger = Logger.getLogger(RewriteService.class);
    public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
    public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";

    public static Map<String, List<Rewrite>> executeRewriteCmd(Config config, Rule rule) {
        Map<String, List<Rewrite>> ruleResults = new HashMap<>();

        // Parse first the Rule condition to populate the Query object using the YAML Condition query
        // See the parser maven project for examples, unit tests
        QueryVisitor visitor = QueryUtils.parseAndVisit(rule.when().Condition());

        /*
           Handle the 3 supported cases where the query contains:

           - One clause: FIND java.annotation WHERE (name='@SpringBootApplication')

           - Clauses separated with the OR operator:

             FIND java.annotation WHERE (name='@SpringBootApplication') OR
                  java.annotation WHERE (name='@Deprecated')

           - Clauses separated with the AND operator:

             FIND java.annotation WHERE (name='@SpringBootApplication') AND
                  pom.dependency WHERE (groupId='org.springframework.boot', artifactId='spring-boot', version='3.4.2')

         */
        if (visitor.getSimpleQueries().size() == 1) {
            visitor.getSimpleQueries().stream().findFirst().ifPresent(q -> {
                List<Rewrite> results = executeQueryCommand(config, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else if (visitor.getOrQueries().size() > 1) {
            visitor.getOrQueries().stream().forEach(q -> {
                List<Rewrite> results = executeQueryCommand(config, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else if (visitor.getAndQueries().size() > 1) {
            visitor.getAndQueries().stream().forEach(q -> {
                List<Rewrite> results = executeQueryCommand(config, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else {
            logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
            ruleResults.put(rule.ruleID(), new ArrayList<>());
        }

        return ruleResults;
    }

    private static List<Rewrite> executeQueryCommand(Config config, Rule rule, Query q) {
        // Map the Query to the RecipeDTO
        RecipeDTO dto = QueryToRecipeMapper.map(q);
        logger.infof("Recipe dto: %s", dto);

        // Create the Composite recipe and generate Recipe YAML
        HashMap<String, HashMap<String, String>> recipe = new LinkedHashMap<>();
        recipe.put(
            dto.name(),
            dto.parameters().stream()
            .collect(Collectors.toMap(
                Parameter::parameter,
                Parameter::value,
                (v1, v2) -> v2,
                LinkedHashMap::new
            )));

        CompositeRecipe compositeRecipe = new CompositeRecipe(
            "specs.openrewrite.org/v1beta/recipe",
            "dev.snowdrop.openrewrite.MatchConditions",
            "Try to match a resource",
            "Try to match a resource.",
            Arrays.asList(recipe)
        );

        String yamlRecipe = "";
        try {
            yamlRecipe = yamlRecipeMapper().writeValueAsString(compositeRecipe);
            // logger.debugf("Recipe generated: %s",yamlRecipe);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Format the recipe
        // String yamlRecipe = String.format("""
        //     type: specs.openrewrite.org/v1beta/recipe
        //     name: dev.snowdrop.openrewrite.MatchConditions
        //     displayName: Try to match a resource
        //     recipeList:
        //     - %s
        //     """,
        //     recipe);
        //
        logger.infof("recipeList: %s", yamlRecipe);

        // Copy the rewrite yaml file under the project to scan
        String rewriteYamlName = "rewrite.yml";
        Path yamlFilePath = Paths.get(config.appPath()).resolve(rewriteYamlName);

        try {
            Files.write(yamlFilePath, yamlRecipe.getBytes(),
                StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Recipe's jar files
        String gavs = "org.openrewrite:rewrite-java:8.62.4,org.openrewrite.recipe:rewrite-java-dependencies:1.43.0,dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT";

        // Execute the maven rewrite goal command
        boolean succeed = execMvnCmd(config.appPath(),true,gavs,rewriteYamlName);

        // Populate the result's array
        List<Rewrite> results = new ArrayList<>();
        if (!succeed) {
            logger.warnf("Failed to execute the maven command");
        }

        // TODO: Add logic to scan the csv files generated ...
        // Create a dummy response
        results.add(new Rewrite(dto.id().toString(),dto.name()));
        return results;
    }

    private static ObjectMapper yamlRecipeMapper() {
        YAMLFactory factory = new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        ObjectMapper yamlMapper = new ObjectMapper(factory);

        SimpleModule module = new SimpleModule();
        module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
        yamlMapper.registerModule(module);

        return yamlMapper;
    }

    // TODO The following code is similar with the transform command is is a duplicate
    private static boolean execMvnCmd(String AppProjectPath, boolean verbose, String gavs, String rewriteYamlName) {
        try {
            List<String> command = new ArrayList<>();
            String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

            command.add("mvn");
            command.add("-B");
            command.add("-e");
            command.add(String.format("%s:%s:%s:%s",
                MAVEN_OPENREWRITE_PLUGIN_GROUP,
                MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
                "6.22.1", // TODO: Remove hard coded value of the version => use config property
                "dryRun"));
            // TODO: Remove the hard coded activeRecipes value
            command.add(String.format("-Drewrite.activeRecipes=%s", "dev.snowdrop.openrewrite.MatchConditions"));
            command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
            command.add("-Drewrite.exportDatatables=true");
            command.add(String.format("-DreportOutputDirectory=target/%s", outputDirectoryRewriteName));
            command.add(String.format("-Drewrite.configLocation=%s", rewriteYamlName));

            String commandStr = String.join(" ", command);
            logger.infof("Executing command: %s", commandStr);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(Paths.get(AppProjectPath).toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read and log output in real-time
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (verbose) {
                        logger.infof("      %s", line);
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            logger.errorf("Failed to execute maven command: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
