package dev.snowdrop.mtool.tests.analyze.rewrite.match;

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

class ShouldMatchJavaAnnotationWithOr extends BaseRulesTest {

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
	@CsvSource({"or-query/annotation-or-annotation-match.yaml"})
	void shouldMatchJavaAnnotationWithOr(String ruleSubPath) throws IOException {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> matchList = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(matchList);
		assertTrue(matchList.containsKey("annotation-or-annotation-match"));

		var matchResults = matchList.get("annotation-or-annotation-match");
		assertNotNull(matchResults);

		// Stream through matchResults to find annotations containing Controller or GetMapping
		boolean hasControllerAnnotation = matchResults.stream().map(match -> (String) match.result())
				.anyMatch(result -> result.contains("org.springframework.stereotype.Controller"));

		boolean hasGetMappingAnnotation = matchResults.stream().map(match -> (String) match.result())
				.anyMatch(result -> result.contains("org.springframework.web.bind.annotation.GetMapping"));

		// Assert that at least one of the required annotations is found
		assertTrue(hasControllerAnnotation || hasGetMappingAnnotation,
				"Expected to find either Controller or GetMapping annotation in the results");
	}

}