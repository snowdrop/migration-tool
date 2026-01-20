package dev.snowdrop.mtool.tests.analyze.rewrite.match;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.scanner.CodeScannerService;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import dev.snowdrop.mtool.tests.analyze.BaseRulesTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.table.SearchResults;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.junit.jupiter.api.Assertions.*;

class ShouldMatchPropertiesKey extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	@TempDir
	Path tempDir;

	String jdtls = "";

	@BeforeEach
	void setUp() throws Exception {
		// Copy the code of the project to analyze within the temp dir
		String applicationToScan = "spring-boot-todo-app";
		Path destinationPath = tempDir.resolve(applicationToScan);
		copyFolder(applicationToScan, destinationPath);

		// Copy the rules to be evaluated the temp dir
		String cookBook = "test-rules";
		rulesPath = tempDir.resolve(cookBook);
		copyFolder(cookBook, rulesPath);

		// Configure the test with parameters
		config = createTestConfig(destinationPath, rulesPath, jdtls);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@ParameterizedTest
	@CsvSource({"properties/spring-datasource-properties.yaml"})
	void shouldMatchPropertiesWithScannerRewrite(String ruleSubPath) throws IOException {
		// When condition: properties.key is 'spring.datasource.*'
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("springboot-datasource-config"));
		assertEquals(4, result.get("springboot-datasource-config").size());

		Match match = result.get("springboot-datasource-config").getFirst();
		assertNotNull(match);
		String record = (String) match.result();
		assertEquals(true, record.contains("src/main/resources/application.properties"));
		assertEquals(true, record.contains("Find property `spring.datasource.*`"));
	}

}