package dev.snowdrop.mtool.tests.analyze.rewrite.nomatch;

import dev.snowdrop.mtool.tests.analyze.BaseRulesTest;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.scanner.CodeScannerService;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.junit.jupiter.api.Assertions.*;

class ShouldNotMatchJavaAnnotationWithOr extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	@TempDir
	Path tempDir;

	Path rulesPath;
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
	@CsvSource({"or-query/annotation-or-annotation-no_match.yaml"})
	void shouldNotMatchJavaAnnotationWithOr(String ruleSubPath) throws IOException {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(result);

		// Check if there are any results for this rule
		if (result.containsKey("annotation-or-annotation-no_match")) {
			var matchResults = result.get("annotation-or-annotation-no_match");
			assertNotNull(matchResults);

			// Stream through matchResults to verify no matches for invalid annotations
			boolean hasInvalidControllerAnnotation = matchResults.stream().map(match -> (String) match.result())
					.anyMatch(csvRecord -> csvRecord.contains("org.springframework.stereotype.Controllerr"));

			boolean hasInvalidGetMappingAnnotation = matchResults.stream().map(match -> (String) match.result())
					.anyMatch(csvRecord -> csvRecord.contains("org.springframework.web.bind.annotation.GetMappinggg"));

			// Assert that no invalid annotations are found
			assertFalse(hasInvalidControllerAnnotation, "Should not find matches for invalid annotation 'Controllerr'");
			assertFalse(hasInvalidGetMappingAnnotation,
					"Should not find matches for invalid annotation 'GetMappinggg'");
		} else {
			// This is the expected behavior - no matches should be found for invalid annotations
			System.out.println("No matches found for invalid annotations - this is expected behavior");
		}
	}

}