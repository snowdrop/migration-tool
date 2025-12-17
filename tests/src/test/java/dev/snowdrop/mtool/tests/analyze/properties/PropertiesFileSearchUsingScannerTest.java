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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesFileSearchUsingScannerTest extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	protected FileSearch fileSearcher;
	protected FileSearch contentSearcher;

	Path applicationPath;

	@TempDir
	Path tempDir;

	String jdtls = "";

	@BeforeEach
	void setUp() throws Exception {
		// Copy the code of the project to analyze within the temp dir
		String applicationToScan = "spring-boot-todo-app";
		applicationPath = tempDir.resolve(applicationToScan);
		copyFolder(applicationToScan, applicationPath);

		fileSearcher = new FileSearch();
		contentSearcher = new FileSearch();

		// Copy the rules to be evaluated the temp dir
		String cookBook = "test-rules";
		rulesPath = tempDir.resolve(cookBook);
		copyFolder(cookBook, rulesPath);

		// Configure the test with parameters
		config = createTestConfig(applicationPath, rulesPath, jdtls);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@ParameterizedTest
	@CsvSource({"properties/spring-datasource-properties.yaml"})
	void shouldMatchJavaAnnotationWithScannerRewrite(String ruleSubPath) throws IOException {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("springboot-datasource-config"));
		//
		List<Match> matches = result.get("springboot-datasource-config");
		assertNotNull(matches);

		assertThat(result.get("springboot-datasource-config")).hasSize(4);

		var results = result.get("springboot-datasource-config");
		assertThat(results).extracting(Match::result).contains(
				"resources/application.properties:5 | spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo",
				"resources/application.properties:6 | spring.datasource.username=root",
				"resources/application.properties:7 | spring.datasource.password=root",
				"resources/application.properties:8 | spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
	}
}