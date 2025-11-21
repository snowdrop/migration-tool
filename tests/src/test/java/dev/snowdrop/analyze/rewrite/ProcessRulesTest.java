package dev.snowdrop.analyze.rewrite;

import dev.snowdrop.analyze.BaseRulesTest;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.CodeScannerService;
import dev.snowdrop.analyze.services.ScanCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFolder;
import static org.junit.jupiter.api.Assertions.*;

class ProcessRulesTest extends BaseRulesTest {

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
	@CsvSource({
        "simple-query/java-annotation-rewrite.yaml"
    })
	void shouldMatchJavaAnnotationWithScannerRewrite(String ruleSubPath) throws IOException {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(result);
        assertTrue(result.containsKey("java-annotation-controller-found"));

        Match match = result.get("java-annotation-controller-found").getFirst();
        assertNotNull(match);

        String csvRecord = (String)match.result();
        assertEquals(true,csvRecord.contains("org.springframework.stereotype.Controller"));
        assertEquals(true,csvRecord.contains("dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv"));
        assertEquals(true,csvRecord.contains("JAVA.ANNOTATION"));
	}

}