package dev.snowdrop.mtool.tests.analyze.properties;

import dev.snowdrop.mtool.tests.analyze.BaseRulesTest;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.scanner.CodeScannerService;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import dev.snowdrop.mtool.scanner.file.FileSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesKeyUsingRewriteScannerTest extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	protected FileSearch fileSearcher;
	protected FileSearch contentSearcher;

	String applicationToScan = "spring-boot-todo-app";
	Path applicationPath;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() throws Exception {
		// Copy the code of the project to analyze within the temp dir
		applicationPath = tempDir.resolve(applicationToScan);
		copyFolder(applicationToScan, applicationPath);

		fileSearcher = new FileSearch();
		contentSearcher = new FileSearch();

		// Copy the rules to be evaluated the temp dir
		String cookBook = "test-rules";
		rulesPath = tempDir.resolve(cookBook);
		copyFolder(cookBook, rulesPath);

		// Configure the test with parameters
		config = createTestConfig(applicationPath, rulesPath);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@ParameterizedTest
	@CsvSource({"properties/spring-datasource-properties.yaml"})
	void shouldMatchDataSourcePropertiesWithScannerRewrite(String ruleSubPath) throws Exception {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));
		rules.forEach(r -> {
			System.out.printf("## Rule: %s\n", r.when().condition());
		});

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		runCat(Path.of(tempDir.toString(), applicationToScan, "target/rewrite/rewrite.patch"));
		result.forEach((match, matches) -> {
			System.out.printf("### match: %s\n", match);
		});

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("springboot-datasource-config"));

		List<Match> matches = result.get("springboot-datasource-config");
		assertNotNull(matches);

		System.out.println("#### Show matches ....");
		matches.stream().map(Match::result).map(Object::toString).flatMap(s -> Arrays.stream(s.split("\\|")))
				.forEach(System.out::println);

		//assertThat(matches).hasSize(4);
		assertThat(matches).extracting(Match::result).map(Object::toString) // Convert the object to string first
				.flatExtracting(s -> Arrays.asList(s.split("\\|")))
				.contains("src/main/resources/application.properties",
						"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
						"spring.datasource.password=root",
						"spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
	}

	@ParameterizedTest
	@CsvSource({"properties/spring-datasource-or-jpa-properties.yaml"})
	void shouldMatchDataSourceOrJpaPropertiesWithScannerRewrite(String ruleSubPath) throws Exception {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));
		rules.forEach(r -> {
			System.out.printf("## Rule: %s\n", r.when().condition());
		});

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		runCat(Path.of(tempDir.toString(), applicationToScan, "target/rewrite/rewrite.patch"));
		result.forEach((match, matches) -> {
			System.out.printf("### match: %s\n", match);
		});

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("springboot-datasource-or-jpa-properties"));

		List<Match> matches = result.get("springboot-datasource-or-jpa-properties");
		assertNotNull(matches);

		System.out.println("#### Show matches ....");
		matches.stream().map(Match::result).map(Object::toString).flatMap(s -> Arrays.stream(s.split("\\|")))
				.forEach(System.out::println);

		//assertThat(matches).hasSize(4);
		assertThat(matches).extracting(Match::result).map(Object::toString) // Convert the object to string first
				.flatExtracting(s -> Arrays.asList(s.split("\\|")))
				.contains("src/main/resources/application.properties",
						"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
						"spring.datasource.password=root",
						"spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
	}
}