package dev.snowdrop.analyze.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.CsvRecord;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RewriteService {
	private static final Logger logger = Logger.getLogger(RewriteService.class);
	public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
	public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";

	private final Config config;

	public RewriteService(Config config) {
		this.config = config;
	}

	public Map<String, List<Rewrite>> executeRewriteCmd(Rule rule) {
		Map<String, List<Rewrite>> ruleResults = new HashMap<>();

		// Parse first the Rule condition to populate the Query object using the YAML condition query
		// See the parser maven project for examples, unit tests
		QueryVisitor visitor = QueryUtils.parseAndVisit(rule.when().condition());

		/*
		 * Handle the 3 supported cases where the query contains:
		 *
		 * - One clause: java.annotation is '@SpringBootApplication'
		 *
		 * - Clauses separated with the OR operator:
		 *
		 * java.annotation is '@SpringBootApplication' OR java.annotation is '@Deprecated'
		 *
		 * - Clauses separated with the AND operator:
		 *
		 * java.annotation is '@SpringBootApplication' AND pom.dependency is (groupId='org.springframework.boot',
		 * artifactId='spring-boot', version='3.4.2')
		 *
		 * See grammar definition:
		 * https://raw.githubusercontent.com/snowdrop/migration-tool/refs/heads/main/parser/src/main/antlr4/Query.g4
		 */
		if (visitor.getSimpleQueries().size() == 1) {
			visitor.getSimpleQueries().stream().findFirst().ifPresent(q -> {
				List<Rewrite> results = executeQueryCommand(config, Collections.singleton(q));
				ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
					List<Rewrite> combined = new ArrayList<>(existing);
					combined.addAll(newResults);
					return combined;
				});
			});
		} else if (visitor.getOrQueries().size() > 1) {
			List<Rewrite> results = executeQueryCommand(config, visitor.getOrQueries());
			ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
				List<Rewrite> combined = new ArrayList<>(existing);
				combined.addAll(newResults);
				return combined;
			});
		} else if (visitor.getAndQueries().size() > 1) {
			// TODO: To be tested
			List<Rewrite> results = executeQueryCommand(config, visitor.getAndQueries());
			ruleResults.merge(rule.ruleID(), results, (existing, newResults) -> {
				List<Rewrite> combined = new ArrayList<>(existing);
				combined.addAll(newResults);
				return combined;
			});
		} else {
			logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
			ruleResults.put(rule.ruleID(), new ArrayList<>());
		}

		return ruleResults;
	}

	private List<Rewrite> executeQueryCommand(Config config, Set<Query> queries) {

		// Composite recipe - using Map with List to allow multiple entries with same key
		// List<Map<String, Map<String, String>>> recipes = new ArrayList<>();
		List<Object> recipes = new ArrayList<>();
		List<RecipeDTO> recipeDTOs = new ArrayList<>();

		queries.stream().forEach(q -> {
			// Convert the Query to the RecipeDTO
			RecipeDTO dto = QueryToRecipeMapper.map(q);
			recipeDTOs.add(dto);

			logger.debugf("Recipe dto: %s", dto);
		});

		for (RecipeDTO dto : recipeDTOs) {
			Map<String, String> parameters = dto.parameters().stream().collect(
					Collectors.toMap(Parameter::parameter, Parameter::value, (v1, v2) -> v2, LinkedHashMap::new));

			// Add a new, single-entry map to the list
			// recipes.add(Map.of(dto.name(), parameters));

			Map<String, Map<String, String>> singleRecipe = Map.of(dto.name(), parameters);
			recipes.add(singleRecipe);
		}

		CompositeRecipe compositeRecipe = new CompositeRecipe("specs.openrewrite.org/v1beta/recipe",
				"dev.snowdrop.openrewrite.MatchConditions", "Try to match a resource", "Try to match a resource.",
				recipes);

		/*
		 * Render the recipe as YAML type: specs.openrewrite.org/v1beta/recipe name:
		 * dev.snowdrop.openrewrite.MatchConditions displayName: Try to match a resource recipeList: - %s
		 */
		String yamlRecipe = "";
		try {
			yamlRecipe = yamlRecipeMapper().writeValueAsString(compositeRecipe);
			logger.debugf("Recipe generated: %s", yamlRecipe);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Copy the rewrite yaml file under the project to scan
		String rewriteYamlName = "rewrite.yml";
		Path yamlFilePath = Paths.get(config.appPath()).resolve(rewriteYamlName);

		try {
			Files.write(yamlFilePath, yamlRecipe.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
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

		// Iterate through the recipe list to get the results
		List<Rewrite> allResults = new ArrayList<>();
		recipeDTOs.stream().forEach(dto -> {
			// Get from the DTO the matchId to search about
			String matchId = dto.parameters().stream().filter(p -> p.parameter().equals("matchId"))
					.map(Parameter::value).findAny().orElse(null);

			// Populate the results using an array as a symbol can be present several times in files
			// By example; the GetMapping annotation can be used to define several endpoints
			// This array will populate for each match found a Rewrite object
			allResults.addAll(findRecordsMatching(config.appPath(), matchId));
		});
		return allResults;
	}

	private ObjectMapper yamlRecipeMapper() {
		YAMLFactory factory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		ObjectMapper yamlMapper = new ObjectMapper(factory);

		SimpleModule module = new SimpleModule();
		module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
		yamlMapper.registerModule(module);

		return yamlMapper;
	}

	// TODO The following code is similar to the transform maven command.
	// We should investigate if we could have one method able to generate it for analyze and transform
	private static boolean execMvnCmd(String AppProjectPath, boolean verbose, String gavs, String rewriteYamlName) {
		try {
			List<String> command = new ArrayList<>();
			String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

			command.add("mvn");
			command.add("-B");
			command.add("-e");
			command.add(String.format("%s:%s:%s:%s", MAVEN_OPENREWRITE_PLUGIN_GROUP, MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
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
	 * Finds csv records with the matchId of the query using OpenCSV
	 *
	 * @param projectPath
	 *            The path to search for CSV files
	 * @param matchIdToSearch
	 *            The match ID to search for in CSV files
	 *
	 * @return List of Rewrite objects for matching records
	 */
	private List<Rewrite> findRecordsMatching(String projectPath, String matchIdToSearch) {
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
			try (Stream<Path> directories = Files.list(openRewriteCsvPath).filter(Files::isDirectory)) {

				directories.forEach(dateTimeDir -> {
					String parentFolderName = dateTimeDir.getFileName().toString();

					try {
						// List all CSV files in each datetime directory
						try (Stream<Path> csvFiles = Files.list(dateTimeDir).filter(Files::isRegularFile)
								.filter(path -> path.toString().endsWith(".csv"))) {

							csvFiles.forEach(csvFile -> {
								String csvFileName = csvFile.getFileName().toString();

								try (CSVReader csvReader = new CSVReader(new FileReader(csvFile.toFile()))) {
									// Parse CSV using OpenCSV
									List<CsvRecord> records = new CsvToBeanBuilder<CsvRecord>(csvReader)
											.withType(CsvRecord.class).withSkipLines(2) // Skip header and description
											// rows
											.build().parse();

									// Search through records for matching matchId
									for (int i = 0; i < records.size(); i++) {
										CsvRecord record = records.get(i);

										if (record.getMatchId() != null
												&& record.getMatchId().equals(matchIdToSearch)) {
											// Extract type and symbol from CSV name and content
											String fileType = record.getType();
											String symbolType = record.getSymbol();
											String pattern = record.getPattern() != null ? record.getPattern() : "N/A";

											// Build name in the new format:
											// parentFolderName/csvFileName:line_number|pattern.symbol|type
											String name = String.format("%s/%s:%d|%s.%s|%s", parentFolderName,
													csvFileName, i + 3, // Add 3 to account for skipped header rows
													// (0-based index + 2 skipped + 1 for 1-based
													// line numbering)
													pattern, symbolType, fileType);

											results.add(new Rewrite(matchIdToSearch, name));
											logger.infof("Found match in %s at record %d: %s", csvFile, i + 1, name);
										}
									}
								} catch (IOException e) {
									logger.errorf("Error parsing CSV file %s with OpenCSV: %s", csvFile,
											e.getMessage());
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

}
