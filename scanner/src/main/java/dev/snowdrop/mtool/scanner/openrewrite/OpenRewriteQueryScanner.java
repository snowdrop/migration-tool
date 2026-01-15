package dev.snowdrop.mtool.scanner.openrewrite;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.CsvRecord;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.ScannerType;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.model.transform.CompositeRecipe;
import dev.snowdrop.mtool.scanner.QueryScanner;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
		return scansCode(config);
	}

	@Deprecated
	private List<Match> oldMethodToGetMatches(Config config, Query query) {
		logger.infof("OpenRewrite scanner executing 1 query");

		if (config.scanner() != null && !ScannerType.OPENREWRITE.label().equals(config.scanner())) {
			logger.warnf("Query %s.%s is configured for scanner '%s', not 'openrewrite'. Skipping.", query.fileType(),
					query.symbol(), config.scanner());
			return new ArrayList<>();
		}

		CompositeRecipe openRewriteRecipe = parse(query);

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
	private List<Match> scansCode(Config config) {
		logger.infof("OpenRewrite scanner executing 1 query");

		if (config.scanner() != null && !ScannerType.OPENREWRITE.label().equals(config.scanner())) {
			return new ArrayList<>();
		}

		List<String> recipes = getRecipesToApply();

		List<Match> matches = applyRecipes(config, recipes);

		// Execute the maven rewrite goal command
		if (!matches.isEmpty()) {
			logger.warnf("No results found");
			return new ArrayList<>();
		}
		return matches;

	}

	//TODO this method needs to be adapted, for the moment respects the existent needs to display data
	List<Match> findMatchsFromResults(ResultsContainer resultsContainer, List<String> recipes) {
		List<Match> results = new ArrayList<>();
		for (String recipeName : recipes) {
			RecipeRun run = resultsContainer.getRecipeRuns().get(recipeName);
			Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
					.filter(entry -> entry.getKey().getName().contains("SearchResults")).findFirst();

			if (resultMap.isPresent()) {

				List<SearchResults.Row> rows = (List<SearchResults.Row>) resultMap.get().getValue();
				for (SearchResults.Row row : rows) {
					String sourcePath = row.getSourcePath();
					String description = row.getDescription();
					String recipe = row.getRecipe();

					String result = String.format("%s/%s:%d|%s.%s|%s", "parentFolderName", "csvFileName", 42,
							sourcePath, description, recipe);
					results.add(new Match("toBeDone", getScannerType(), result));

				}

			}
		}
		return results;
	}

	private List<Match> applyRecipes(Config config, List<String> recipes) {
		RewriteConfig cfg = new RewriteConfig();
		cfg.setAppPath(Paths.get(config.appPath()));

		cfg.setActiveRecipes(recipes);

		RewriteService svc = new RewriteService(cfg);
		svc.init();
		ResultsContainer run = svc.run();
		return findMatchsFromResults(run, recipes);

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

	private CompositeRecipe parse(Query query) {
		return switch (query.symbol()) {
			case "annotation" -> buildAnnotationRecipe(query);
			case "file" -> buildFindSourceFilesRecipe(query);
			//			case "class" -> buildClassRecipe(query);
			//			case "method" -> buildMethodRecipe(query);
			default -> throw new IllegalArgumentException("Unsupported symbol: " + query.symbol());
		};
	}

	private List<String> getRecipesToApply() {
		return List.of("org.openrewrite.java.search.FindAnnotations");

	}

	private CompositeRecipe buildAnnotationRecipe(Query query) {
		String annotationName = query.keyValues().get("name");
		String matchId = UUID.randomUUID().toString();

		Map<String, Map<String, String>> recipe = Map.of("dev.snowdrop.mtool.openrewrite.java.search.FindAnnotations",
				Map.of("pattern", annotationName, "matchId", matchId, "matchOnMetaAnnotations",
						Boolean.FALSE.toString()));

		CompositeRecipe composite = new CompositeRecipe("specs.openrewrite.org/v1beta/recipe",
				OPENREWRITE_MATCH_CONDITIONS, "Try to match a resource", "Try to match a resource", List.of(recipe));

		return composite;
	}

	private CompositeRecipe buildFindSourceFilesRecipe(Query query) {
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