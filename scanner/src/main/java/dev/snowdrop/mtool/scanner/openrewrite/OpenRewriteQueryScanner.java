package dev.snowdrop.mtool.scanner.openrewrite;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.CsvRecord;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.ScannerType;
import dev.snowdrop.mtool.model.openrewrite.CompositeRecipe;
import dev.snowdrop.mtool.model.openrewrite.RecipeDefinition;
import dev.snowdrop.mtool.model.openrewrite.RecipeHolder;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.scanner.QueryScanner;
import dev.snowdrop.logging.LoggingService;
import dev.snowdrop.openrewrite.cli.RewriteService;
import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import org.jboss.logging.Logger;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SearchResults;

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

/**
 * Scanner implementation for Java-related queries using OpenRewrite.
 * Handles queries like java.annotation, java.referenced, java.method, etc.
 */
public class OpenRewriteQueryScanner implements QueryScanner {

	private static final Logger logger = Logger.getLogger(OpenRewriteQueryScanner.class);
	public static final String MAVEN_OPENREWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
	public static final String MAVEN_OPENREWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";
	public static final String OPENREWRITE_MATCH_CONDITIONS = "dev.snowdrop.openrewrite.MatchConditions";
	private static final String KEY_VALUE_DELIMITER = "=";

	private static boolean resourcesLoaded = false;
	private static RewriteService rewriteServiceInstance;

	@Deprecated
	@Override
	public List<Match> executeQueries(Config config, Set<Query> queries) {
		logger.infof("OpenRewrite scanner executing %d queries", queries.size());

		List<Match> allResults = new ArrayList<>();

		for (Query q : queries) {
			List<Match> partial = scansCodeFor(config, q);

			if (partial != null && !partial.isEmpty()) {
				allResults.addAll(partial);
			}
		}

		logger.infof("OpenRewrite scanner completed. Total matches found: %d", allResults.size());
		return allResults;
	}

	@Override
	public List<Match> scansCodeFor(Config config, Query query) {
		return scansCode(config, query);
	}

	@Deprecated
	private List<Match> oldMethodToGetMatches(Config config, Query query) {
		logger.infof("OpenRewrite scanner executing 1 query");

		if (config.scanner() != null && !ScannerType.OPENREWRITE.label().equals(config.scanner())) {
			logger.warnf("Query %s.%s is configured for scanner '%s', not 'openrewrite'. Skipping.", query.fileType(),
					query.symbol(), config.scanner());
			return new ArrayList<>();
		}

		CompositeRecipe openRewriteRecipe = oldPparse(query);

		String yamlRecipe = toYaml(openRewriteRecipe);
		logger.debugf("Recipe generated: %s", yamlRecipe);

		// Execute the maven rewrite goal command
		boolean succeed = execOpenrewriteMvnPlugin(config, true, yamlRecipe);
		if (!succeed) {
			logger.warnf("Failed to execute the maven command");
			return new ArrayList<>();
		}

		String matchId = extractMatchId(openRewriteRecipe);
		List<Match> results = findRecordsMatching(config.appPath(), matchId);

		logger.debugf("Found %d matches for query %s.%s ", results.size(), query.fileType(), query.symbol());

		logger.infof("OpenRewrite scanner completed. Total matches found: %d", results.size());
		return results;
	}

	/****
	 * New method using the RewriteClient.
	 * @param config
	 * @return
	 */
	private List<Match> scansCode(Config config, Query q) {
		logger.infof("OpenRewrite scanner executing 1 query");

		if (config.scanner() != null && !ScannerType.OPENREWRITE.label().equals(config.scanner())) {
			return new ArrayList<>();
		}

		/*
		   Create from the query its corresponding Openrewrite Recipe
		   The RecipeHolder handles the definition of the Recipe like the list of the Java Recipe class
		   to be executed.

		   Remark: This is similar to a YAML Recipes file

		   type: specs.openrewrite.org/v1beta/recipe
		   name: dev.snowdrop.mtool.openrewrite.ConditionToMatch
		   displayName: Search a Java annotation
		   description: Search a Java annotation.
		   recipeList:
		     - org.openrewrite.java.search.FindAnnotations:
		         annotationPattern:
		         matchMetaAnnotations: false

		 */
		RecipeHolder recipeHolder = parse(q);

		List<Match> matches;
		try {
			matches = applyRecipes(config, recipeHolder);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (matches.isEmpty()) {
			logger.warnf("No results found");
			return new ArrayList<>();
		}
		return matches;

	}

	//TODO this method needs to be adapted, for the moment respects the existent needs to display data
	List<Match> findMatchsFromResults(ResultsContainer resultsContainer, RecipeDefinition recipeDefinition) {
		List<Match> results = new ArrayList<>();

		if (!resultsContainer.isNotEmpty()) {
			System.out.println("### No match found for: " + recipeDefinition.getFqName());
			return results;
		}

		RecipeRun run = resultsContainer.getRecipeRuns().get(recipeDefinition.getFqName());
		Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
				.filter(entry -> entry.getKey().getName().contains("SearchResults")).findFirst();

		if (resultMap.isPresent()) {

			List<SearchResults.Row> rows = (List<SearchResults.Row>) resultMap.get().getValue();
			for (SearchResults.Row row : rows) {

				// The source path of the file with the search result markers present.
				String sourcePath = row.getSourcePath();

				// A recipe may modify the source path. This is the path after the run. null when a source file was deleted during the run.
				// TODO: Do we need it when we do a search ?
				String afterSourcePath = row.getAfterSourcePath();

				// The specific recipe that added the Search marker.
				String recipe = row.getRecipe();

				// The content of the description of the marker.
				String description = row.getRecipe();

				// The trimmed printed tree of the LST element that the marker is attached to.
				String result = row.getResult();

				String formatedResult = String.format("%s|%s|%s|%s", sourcePath, result, description, recipe);
				System.out.println("### Match result: " + formatedResult);
				results.add(new Match("toBeDone", getScannerType(), formatedResult));
			}
		} else {
			System.out.println("### No SearchResults DataTable found for: " + recipeDefinition.getFqName());
		}
		return results;
	}

	private List<Match> applyRecipes(Config config, RecipeHolder recipeHolder) throws Exception {
		RewriteConfig cfg = new RewriteConfig();
		cfg.setAppPath(Paths.get(config.appPath()));

		// Configure the RewriteConfig using the RecipeHolder
		// The fqName corresponds to the fully qualify name of the Java Recipe class to be executed
		RecipeDefinition rd = recipeHolder.getRecipesList().getFirst();
		cfg.setFqNameRecipe(rd.getFqName());

		// Set the parameters needed to configure the fields of the Java Recipe Class
		cfg.setRecipeOptions(convertMapParametersToKeyValueSet(rd.getFieldMappings()));

		System.out.println("### Running recipe for : " + cfg.getFqNameRecipe());
		System.out.println("###                with : " + cfg.getRecipeOptions());

		/*
		  Previous code which has been replaced with the RewriteService singleton

		  This code works as for each recipe we parse the code, create the sourceSet and reset the DataTables, etc every time
		  RewriteService svc = new RewriteService(cfg);
		  svc.init();
		 */

		// Create a singleton instance of the RewriteService to allow to load only one time all the resources of a project to scan
		RewriteService svc = createRewriteServiceInstance(cfg);
		/*
		   If the SourceSet (= Tree of openrewrite J* classes) has been created, then that means that the RewriteService
		   has been also initialized, that a Context and Environment exist too.
		   It is only need in this case to update the config as we are processing a new recipe
		   and to create a new Context to reset the Map of the DataTables otherwise the Context will continue to aggregate them
		   See: https://github.com/snowdrop/migration-tool/issues/212
		*/
		if (svc.isSourceSetInitialized()) {
			svc.createExecutionContext();
			svc.updateConfig(cfg);
		} else {
			svc.init();
		}

		ResultsContainer run = svc.run();
		return findMatchsFromResults(run, rd);
	}

	public static RewriteService createRewriteServiceInstance(RewriteConfig cfg) {
		if (rewriteServiceInstance == null) {
			System.out.println("### Return RewriteService: NEW");
			rewriteServiceInstance = new RewriteService(cfg);
			rewriteServiceInstance.setLogger(new LoggingService());
		}
		System.out.println("### Return RewriteService: EXISTING");
		return rewriteServiceInstance;
	}

	/**
	 * Converts a Map of field parameter and value into a Set of "k=v" strings.
	 */
	public Set<String> convertMapParametersToKeyValueSet(Map<String, String> fieldsByName) {
		if (fieldsByName == null || fieldsByName.isEmpty()) {
			return Set.of();
		}

		return fieldsByName.entrySet().stream().map(entry -> entry.getKey() + KEY_VALUE_DELIMITER + entry.getValue())
				.collect(Collectors.toSet());
	}

	public Map<String, String> convertKeyValueToFieldParameters(Set<String> rawEntries) {
		return rawEntries.stream()
				// Filter out any strings that don't contain '=' to avoid errors
				.filter(s -> s != null && s.contains("="))
				.collect(Collectors.toMap(s -> s.substring(0, s.indexOf("=")).trim(), // Key
						s -> s.substring(s.indexOf("=") + 1).trim(), // Value
						// Merge function: in case of duplicate keys, keep the existing one
						(existing, replacement) -> existing));
	}

	private String extractMatchId(CompositeRecipe composite) {
		Object first = composite.recipeList().get(0);

		Map<String, Map<String, String>> recipeMap = (Map<String, Map<String, String>>) first;

		Map<String, String> inner = recipeMap.values().iterator().next();

		return inner.get("matchId");
	}

	private Map<String, String> extractParams(CompositeRecipe composite) {
		Object first = composite.recipeList().get(0);

		if (!(first instanceof Map<?, ?> rawMap)) {
			throw new IllegalStateException("recipeList must contain rawMap");
		}

		// Recipe's jar files
		String gavs = "org.openrewrite:rewrite-java:8.71.0,"
				+ "org.openrewrite.recipe:rewrite-java-dependencies:1.49.0,"
				+ "dev.snowdrop.mtool:openrewrite-recipes:1.0.4";
		Map<String, Map<String, String>> recipeMap = (Map<String, Map<String, String>>) first;

		return recipeMap.values().iterator().next();

	}

	private String extractEntryKey(CompositeRecipe recipe) {
		Object first = recipe.recipeList().get(0);

		if (!(first instanceof Map<?, ?> rawMap)) {
			throw new IllegalStateException("recipeList must contain rawMap");
		}

		Map<String, Map<String, String>> recipeMap = rawMap.entrySet().stream()
				.collect(Collectors.toMap(e -> (String) e.getKey(), e -> (Map<String, String>) e.getValue()));

		var entry = recipeMap.entrySet().iterator().next();

		return entry.getKey();

	}

	private RecipeHolder parse(Query query) {
		return switch (query.fileType() + "." + query.symbol()) {
			case "java.annotation" -> buildSearchAnnotationRecipe(query);
			case "source.file" -> buildFindSourceFilesRecipe(query);
			case "properties.key" -> buildFindProperties(query);
			default -> throw new IllegalArgumentException("Unsupported symbol: " + query.symbol());
		};
	}

	private RecipeHolder buildFindProperties(Query query) {
		String propertyKey = query.keyValues().get("value");

		RecipeHolder recipeHolder = new RecipeHolder().withName("dev.snowdrop.mtool.openrewrite.ConditionToMatch")
				.withDisplayName("Search an application property").withDescription("Search an application property.");

		HashMap<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put("propertyKey", propertyKey);

		recipeHolder.setRecipesList(List.of(
				new RecipeDefinition().withFullyQualifyRecipeName("org.openrewrite.properties.search.FindProperties")
						.withFieldMappings(fieldMappings)));

		return recipeHolder;
	}

	private RecipeHolder buildSearchAnnotationRecipe(Query query) {
		String annotationName = query.keyValues().get("name");

		RecipeHolder recipeHolder = new RecipeHolder().withName("dev.snowdrop.mtool.openrewrite.ConditionToMatch")
				.withDisplayName("Search a Java annotation").withDescription("Search a Java annotation.");

		HashMap<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put("annotationPattern", annotationName);
		fieldMappings.put("matchMetaAnnotations", Boolean.FALSE.toString());

		recipeHolder.setRecipesList(
				List.of(new RecipeDefinition().withFullyQualifyRecipeName("org.openrewrite.java.search.FindAnnotations")
						.withFieldMappings(fieldMappings)));

		return recipeHolder;
	}

	private RecipeHolder buildFindSourceFilesRecipe(Query query) {
		String filePattern = query.keyValues().get("value");

		RecipeHolder recipeHolder = new RecipeHolder().withName("dev.snowdrop.mtool.openrewrite.ConditionToMatch")
				.withDisplayName("Find files within the code source").withDescription(
						"Find files within the code source. Paths are always interpreted as relative to the repository root.");

		HashMap<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put("filePattern", filePattern);

		recipeHolder.setRecipesList(List.of(new RecipeDefinition()
				.withFullyQualifyRecipeName("org.openrewrite.FindSourceFiles").withFieldMappings(fieldMappings)));

		return recipeHolder;
	}

	@Deprecated
	private CompositeRecipe oldPparse(Query query) {
		return switch (query.symbol()) {
			case "annotation" -> oldBuildAnnotationRecipe(query);
			// TODO : case "file" -> buildFindSourceFilesRecipe(query);
			default -> throw new IllegalArgumentException("Unsupported symbol: " + query.symbol());
		};
	}

	@Deprecated
	private List<String> getRecipesToApply() {
		return List.of("org.openrewrite.java.search.FindAnnotations");
	}

	@Deprecated
	private CompositeRecipe oldBuildAnnotationRecipe(Query query) {
		String annotationName = query.keyValues().get("name");
		String matchId = UUID.randomUUID().toString();

		Map<String, Map<String, String>> recipe = Map.of("dev.snowdrop.mtool.openrewrite.java.search.FindAnnotations",
				Map.of("pattern", annotationName, "matchId", matchId, "matchOnMetaAnnotations",
						Boolean.FALSE.toString()));

		CompositeRecipe composite = new CompositeRecipe("specs.openrewrite.org/v1beta/recipe",
				OPENREWRITE_MATCH_CONDITIONS, "Try to match a resource", "Try to match a resource", List.of(recipe));

		return composite;
	}

	@Deprecated
	private CompositeRecipe oldBuildFindSourceFilesRecipe(Query query) {
		String filePattern = query.keyValues().get("value");
		String matchId = UUID.randomUUID().toString();

		Map<String, Map<String, String>> recipe = Map.of("dev.snowdrop.mtool.openrewrite.file.search.FindSourceFiles",
				Map.of("filePattern", filePattern, "matchId", matchId));

		CompositeRecipe composite = new CompositeRecipe("specs.openrewrite.org/v1beta/recipe",
				"dev.snowdrop.openrewrite.MatchConditions",
				"Find files by source path. Paths are always interpreted as relative to the repository root.",
				"Try to match a resource", List.of(recipe));

		return composite;
	}

	@Override
	public String getScannerType() {
		return "openrewrite";
	}

	@Override
	public boolean supports(Query query) {
		String symbol = query.symbol();
		String fileType = query.fileType();
		return (fileType.contains("java") && symbol.contains("annotation"))
				|| (fileType.contains("properties") && symbol.contains("key"))
				|| (fileType.contains("source") && symbol.contains("file"));
	}

	public String toYaml(CompositeRecipe recipe) {
		StringBuilder yaml = new StringBuilder();
		yaml.append("type: ").append("\"").append(recipe.type()).append("\"").append("\n");
		yaml.append("name: ").append("\"").append(recipe.name()).append("\"").append("\n");
		yaml.append("displayName: ").append("\"").append(recipe.displayName()).append("\"").append("\n");
		yaml.append("description: ").append("\"").append(recipe.description()).append("\"").append("\n");
		yaml.append("recipeList:\n");

		String entryKey = extractEntryKey(recipe);
		yaml.append("  - ").append(entryKey).append(":\n");

		extractParams(recipe).forEach((key, value) -> {
			yaml.append("      ").append(key).append(": ");
			yaml.append("\"").append(value).append("\"");
			yaml.append("\n");
		});
		logger.debugf("Recipe generated: %s", yaml.toString());

		return yaml.toString();
	}

	private boolean execOpenrewriteMvnPlugin(Config config, boolean verbose, String yamlRecipe) {
		// Copy the rewrite yaml file under the project to scan
		String rewriteYamlName = "rewrite.yml";
		Path path = Paths.get(config.appPath());
		Path yamlFilePath = path.resolve(rewriteYamlName);

		try {
			Files.write(yamlFilePath, yamlRecipe.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.errorf("Error writing rewrite YAML file: %s", e.getMessage());
			return false;
		}

		// Recipe's jar files
		String gavs = "org.openrewrite:rewrite-java:8.65.0,"
				+ "org.openrewrite.recipe:rewrite-java-dependencies:1.44.0,"
				+ "dev.snowdrop.mtool:openrewrite-recipes:1.0.5-SNAPSHOT";

		try {
			List<String> command = new ArrayList<>();
			String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));

			command.add("mvn");
			command.add("-B");
			command.add("-e");
			command.add(String.format("%s:%s:%s:%s", MAVEN_OPENREWRITE_PLUGIN_GROUP, MAVEN_OPENREWRITE_PLUGIN_ARTIFACT,
					config.openRewriteMavenPluginVersion(), "dryRun"));
			command.add(String.format("-Drewrite.activeRecipes=%s", "dev.snowdrop.openrewrite.MatchConditions"));
			command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
			command.add("-Drewrite.exportDatatables=true");
			command.add(String.format("-DreportOutputDirectory=target/%s", outputDirectoryRewriteName));
			command.add(String.format("-Drewrite.configLocation=%s", rewriteYamlName));

			String commandStr = String.join(" ", command);
			logger.infof("Executing OpenRewrite command: %s", commandStr);

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(path.toFile());
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

	@Deprecated
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