package dev.snowdrop.analyze.jdtls;

import dev.snowdrop.analyze.BaseRulesTest;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.CodeScannerService;
import dev.snowdrop.analyze.services.ScanCommandExecutor;
import org.eclipse.lsp4j.SymbolInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShouldMatchJavaClassTest extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	@TempDir
	Path tempDir;

	Path rulesPath;
	String jdtls;

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

		// Copy the jdt-ls server
		jdtls = "jdt/konveyor-jdtls";
		copyFolder(jdtls, tempDir.resolve(jdtls));

		// Configure the test with the parameters
		String jdtls = tempDir.resolve("jdt").toString();
		config = createTestConfig(destinationPath, rulesPath, jdtls);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@ParameterizedTest
	@CsvSource({"simple-query/java-class-jdtls.yaml"})
	void shouldMatchJavaClassWithScannerJdtLs(String ruleSubPath) throws IOException {
		// Given a path, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Process the rule
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

		// Then
		assertNotNull(result);
		// Should find from the CSV file the rule-id string
		assertEquals(1, result.get("java-class-taskcontroller-found").size());

		Match match = result.get("java-class-taskcontroller-found").get(0);
		assertNotNull(match);

		@SuppressWarnings("unchecked")
		List<SymbolInformation> symbols = (ArrayList<SymbolInformation>) match.result();
		assertEquals(1, symbols.size());

		SymbolInformation si = symbols.getFirst();
		assertNotNull(si);

		assertEquals("TaskController", si.getName());
		assertEquals("Class", si.getKind().name());
	}

}