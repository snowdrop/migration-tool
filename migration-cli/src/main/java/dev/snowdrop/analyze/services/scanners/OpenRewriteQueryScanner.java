package dev.snowdrop.analyze.services.scanners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.CsvRecord;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.mapper.DynamicDTOMapper;
import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.serializer.RecipeDTOSerializer;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;

/**
 * Scanner implementation for Java-related queries using OpenRewrite.
 * Handles queries like java.annotation, java.referenced, java.method, etc.
 */
public class OpenRewriteQueryScanner implements QueryScanner {

	private static final Logger logger = Logger.getLogger(OpenRewriteQueryScanner.class);
	public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
	public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";
	private final QueryScannerMappingLoader queryScannerMappingLoader;

	public OpenRewriteQueryScanner() {
		this.queryScannerMappingLoader = new QueryScannerMappingLoader();
	}

	@Override
	public List<Match> executeQueries(Config config, Set<Query> queries) {
		logger.infof("OpenRewrite scanner executing %d queries", queries.size());

		// Filter queries that are actually configured for OpenRewrite scanner
		List<Query> validQueries = new ArrayList<>();
		List<RecipeDTO> recipeDTOs = new ArrayList<>();

		for (Query query : queries) {
			// Get scanner configuration for this query
			ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());

			// Validate that this query should be handled by OpenRewrite scanner
			if (!"openrewrite".equals(scannerConfig.getScanner())) {
				logger.warnf("Query %s.%s is configured for scanner '%s', not 'openrewrite'. Skipping.",
						query.fileType(), query.symbol(), scannerConfig.getScanner());
				continue;
			}

			// Get the DTO class that should be used for this query
			String dtoClassName = scannerConfig.getDto();
			logger.debugf("Query %s.%s will use DTO: %s", query.fileType(), query.symbol(), dtoClassName);

			validQueries.add(query);

			// TODO: As the name of the class is know here, do we need to get it using scannerConfig.getDto();
			RecipeDTO dto = DynamicDTOMapper.mapToDTO(query, dtoClassName);
			recipeDTOs.add(dto);
			logger.debugf("DTO created using configured DTO %s: %s", dtoClassName, dto);
		}

		// If no valid queries for this scanner, return empty result
		if (validQueries.isEmpty()) {
			logger.infof("No queries configured for OpenRewrite scanner");
			return new ArrayList<>();
		}

		logger.infof("Processing %d queries configured for OpenRewrite scanner", validQueries.size());

		// Composite recipe - using Map with List to allow multiple entries with same key
		List<Object> recipes = new ArrayList<>();

		for (RecipeDTO dto : recipeDTOs) {
			Map<String, String> parameters = dto.parameters().stream().collect(
					Collectors.toMap(Parameter::parameter, Parameter::value, (v1, v2) -> v2, LinkedHashMap::new));

			Map<String, Map<String, String>> singleRecipe = Map.of(dto.name(), parameters);
			recipes.add(singleRecipe);
		}

		CompositeRecipe compositeRecipe = new CompositeRecipe("specs.openrewrite.org/v1beta/recipe",
				"dev.snowdrop.openrewrite.MatchConditions", "Try to match a resource", "Try to match a resource.",
				recipes);

		// Render the recipe as YAML
		String yamlRecipe = "";
		try {
			yamlRecipe = yamlRecipeMapper().writeValueAsString(compositeRecipe);
			logger.debugf("Recipe generated: %s", yamlRecipe);
		} catch (Exception e) {
			logger.errorf("Error generating YAML recipe: %s", e.getMessage());
			return new ArrayList<>();
		}

		// Copy the rewrite yaml file under the project to scan
		String rewriteYamlName = "rewrite.yml";
		Path yamlFilePath = Paths.get(config.appPath()).resolve(rewriteYamlName);

		try {
			Files.write(yamlFilePath, yamlRecipe.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.errorf("Error writing rewrite YAML file: %s", e.getMessage());
			return new ArrayList<>();
		}

		// Recipe's jar files
		String gavs = "org.openrewrite:rewrite-java:8.65.0,org.openrewrite.recipe:rewrite-java-dependencies:1.44.0,dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT";

		// Execute the maven rewrite goal command
		boolean succeed = execMvnCmd(config.appPath(), true, gavs, rewriteYamlName);
		if (!succeed) {
			logger.warnf("Failed to execute the maven command");
			return new ArrayList<>();
		}

		// Iterate through the recipe list to get the results
		List<Match> allResults = new ArrayList<>();
		for (int i = 0; i < recipeDTOs.size(); i++) {
			RecipeDTO dto = recipeDTOs.get(i);
			Query originalQuery = validQueries.get(i);

			// Get from the DTO the matchId to search about
			String matchId = dto.parameters().stream().filter(p -> p.parameter().equals("matchId"))
					.map(Parameter::value).findAny().orElse(null);

			List<Match> queryResults = findRecordsMatching(config.appPath(), matchId);

			// Log the DTO class that was configured for this query type
			ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(originalQuery.fileType(),
					originalQuery.symbol());
			logger.debugf("Found %d matches for query %s.%s (DTO: %s)", queryResults.size(), originalQuery.fileType(),
					originalQuery.symbol(), scannerConfig.getDto());

			allResults.addAll(queryResults);
		}

		logger.infof("OpenRewrite scanner completed. Total matches found: %d", allResults.size());
		return allResults;
	}

	@Override
	public String getScannerType() {
		return "openrewrite";
	}

	@Override
	public boolean supports(Query query) {
		// Check the configuration to see if this query should use the OpenRewrite scanner
		ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
		return "openrewrite".equals(scannerConfig.getScanner());
	}

	private ObjectMapper yamlRecipeMapper() {
		YAMLFactory factory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		ObjectMapper yamlMapper = new ObjectMapper(factory);

		SimpleModule module = new SimpleModule();
		module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
		yamlMapper.registerModule(module);

		return yamlMapper;
	}

	private boolean execMvnCmd(String appProjectPath, boolean verbose, String gavs, String rewriteYamlName) {
		try {
			List<String> command = new ArrayList<>();
			String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

			command.add("mvn");
			command.add("-B");
			command.add("-e");
			command.add(String.format("%s:%s:%s:%s", MAVEN_OPENREWRITE_PLUGIN_GROUP, MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
					"6.22.1", "dryRun"));
			command.add(String.format("-Drewrite.activeRecipes=%s", "dev.snowdrop.openrewrite.MatchConditions"));
			command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
			command.add("-Drewrite.exportDatatables=true");
			command.add(String.format("-DreportOutputDirectory=target/%s", outputDirectoryRewriteName));
			command.add(String.format("-Drewrite.configLocation=%s", rewriteYamlName));

			String commandStr = String.join(" ", command);
			logger.infof("Executing OpenRewrite command: %s", commandStr);

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(Paths.get(appProjectPath).toFile());
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

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
			logger.errorf("Failed to execute OpenRewrite maven command: %s", e.getMessage());
			return false;
		}
	}

	private List<Match> findRecordsMatching(String projectPath, String matchIdToSearch) {
		List<Match> results = new ArrayList<>();
		Path openRewriteCsvPath = Paths.get(projectPath, "target", "rewrite", "datatables");

		try {
			if (!Files.exists(openRewriteCsvPath)) {
				logger.warnf("Datatables directory does not exist: %s", openRewriteCsvPath);
				return results;
			}

			try (Stream<Path> directories = Files.list(openRewriteCsvPath).filter(Files::isDirectory)) {
				directories.forEach(dateTimeDir -> {
					String parentFolderName = dateTimeDir.getFileName().toString();

					try {
						try (Stream<Path> csvFiles = Files.list(dateTimeDir).filter(Files::isRegularFile)
								.filter(path -> path.toString().endsWith(".csv"))) {

							csvFiles.forEach(csvFile -> {
								String csvFileName = csvFile.getFileName().toString();

								try (CSVReader csvReader = new CSVReader(new FileReader(csvFile.toFile()))) {
									List<CsvRecord> records = new CsvToBeanBuilder<CsvRecord>(csvReader)
											.withType(CsvRecord.class).withSkipLines(2).build().parse();

									for (int i = 0; i < records.size(); i++) {
										CsvRecord record = records.get(i);

										if (record.getMatchId() != null
												&& record.getMatchId().equals(matchIdToSearch)) {
											String fileType = record.getType();
											String symbolType = record.getSymbol();
											String pattern = record.getPattern() != null ? record.getPattern() : "N/A";

											String result = String.format("%s/%s:%d|%s.%s|%s", parentFolderName,
													csvFileName, i + 3, pattern, symbolType, fileType);

											// TODO: Deprecated the field name as we will use only the result
											results.add(new Match(matchIdToSearch, getScannerType(), result));
											logger.infof("Found match in %s at record %d: %s", csvFile, i + 1, result);
										}
									}
								} catch (IOException e) {
									logger.errorf("Error parsing CSV file %s: %s", csvFile, e.getMessage());
								}
							});
						}
					} catch (IOException e) {
						logger.errorf("Error listing CSV files in directory %s: %s", dateTimeDir, e.getMessage());
					}
				});
			}
		} catch (IOException e) {
			logger.errorf("Error searching for CSV files: %s", e.getMessage());
			throw new RuntimeException(e);
		}

		return results;
	}
}