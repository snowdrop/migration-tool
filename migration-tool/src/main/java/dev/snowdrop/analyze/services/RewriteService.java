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
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
                    List<Rewrite> combined = new ArrayList<>(existing);
                    combined.addAll(newResults);
                    return combined;
                });
            });
        } else if (visitor.getOrQueries().size() > 1) {
            visitor.getOrQueries().stream().forEach(q -> {
                List<Rewrite> results = executeQueryCommand(config, rule, q);
                ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
                    List<Rewrite> combined = new ArrayList<>(existing);
                    combined.addAll(newResults);
                    return combined;
                });
            });
        } else if (visitor.getAndQueries().size() > 1) {
            visitor.getAndQueries().stream().forEach(q -> {
                List<Rewrite> results = executeQueryCommand(config, rule, q);
                ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
                    List<Rewrite> combined = new ArrayList<>(existing);
                    combined.addAll(newResults);
                    return combined;
                });
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
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Recipe's jar files
        String gavs = "org.openrewrite:rewrite-java:8.65.0,org.openrewrite.recipe:rewrite-java-dependencies:1.44.0,dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT";

        // Execute the maven rewrite goal command
        boolean succeed = execMvnCmd(config.appPath(), true, gavs, rewriteYamlName);
        if (!succeed) {
            logger.warnf("Failed to execute the maven command");
        }

        // Get from the DTO the matchId to search about
        String matchId = dto.parameters().stream()
            .filter(p -> p.parameter().equals("matchId"))
            .map(Parameter::value)
            .findAny()
            .orElse(null);

        // Populate the results using an array as a symbol can be present several times in files
        // By example; the GetMapping annotation can be used to define several endpoints
        // This array will populate for each match found a Rewrite object
        return findRecordsMatching(config.appPath(), matchId);
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

    /**
     * Finds csv records with the matchId of the query
     *
     * @param projectPath The path to search for CSV files
     * @param matchIdToSearch The match ID to search for in CSV files
     * @return List of Rewrite objects for matching records
     */
    private static List<Rewrite> findRecordsMatching(String projectPath, String matchIdToSearch) {
        List<Rewrite> results = new ArrayList<>();

        // Openrewrite folder where CSV files are generated
        Path openRewriteCsvPath = Paths.get(projectPath, "target", "rewrite", "datatables");

        try {
            // Check if the datatables directory exists
            if (!Files.exists(openRewriteCsvPath)) {
                logger.warnf("Datatables directory does not exist: %s", openRewriteCsvPath);
                return results;
            }

            // List all subdirectories (datetime folders)
            try (Stream<Path> directories = Files.list(openRewriteCsvPath)
                    .filter(Files::isDirectory)) {

                directories.forEach(dateTimeDir -> {
                    String parentFolderName = dateTimeDir.getFileName().toString();

                    try {
                        // List all CSV files in each datetime directory
                        try (Stream<Path> csvFiles = Files.list(dateTimeDir)
                                .filter(Files::isRegularFile)
                                .filter(path -> path.toString().endsWith(".csv"))) {

                            csvFiles.forEach(csvFile -> {
                                String csvFileName = csvFile.getFileName().toString();

                                try {
                                    // Read each CSV file and search for the matchId
                                    List<String> lines = Files.readAllLines(csvFile);

                                    for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                                        String line = lines.get(lineIndex);

                                        // Skip header and description rows (first two rows)
                                        if (lineIndex < 2) {
                                            continue;
                                        }

                                        // Parse CSV line and check if matchId matches
                                        if (line.contains(matchIdToSearch)) {
                                            // Parse the CSV to extract the actual Match ID field
                                            String[] fields = parseCsvLine(line);
                                            if (fields.length > 0 && fields[0].equals(matchIdToSearch)) {
                                                // Create name field with parent folder name, CSV file name, and line number
                                                String name = String.format("%s/%s:line_%d",
                                                    parentFolderName, csvFileName, lineIndex + 1);

                                                results.add(new Rewrite(matchIdToSearch, name));
                                                logger.infof("Found match in %s at line %d", csvFile, lineIndex + 1);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    logger.errorf("Error reading CSV file %s: %s", csvFile, e.getMessage());
                                }
                            });
                        }
                    } catch (IOException e) {
                        logger.errorf("Error listing CSV files in directory %s: %s", dateTimeDir, e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            logger.errorf("Error searching for csv files: %s", e.getMessage());
            throw new RuntimeException(e);
        }

        return results;
    }

    /**
     * Simple CSV line parser that handles quoted fields
     * @param line CSV line to parse
     * @return Array of field values
     */
    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

}
