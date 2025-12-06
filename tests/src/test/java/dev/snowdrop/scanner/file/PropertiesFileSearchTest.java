package dev.snowdrop.scanner.file;

import dev.snowdrop.analyze.BaseRulesTest;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.CodeScannerService;
import dev.snowdrop.analyze.services.ScanCommandExecutor;
import dev.snowdrop.analyze.services.scanners.FileSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesFileSearchTest extends BaseRulesTest {

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

		assertThat(result.get("springboot-datasource-config")).hasSize(7);

	}

	//		assertThat(match).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8, 11, 12, 13);
	//
	//		assertThat(results).extracting(Match::).contains(
	//				"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
	//				"spring.datasource.password=root", "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
	//				"spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=true",
	//				"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect");
	//
	//	}

	//	@Test
	//	@DisplayName("Should find the spring properties for datasource and jpa in application.properties")
	//	void testContentSearchForSpringProperties() {
	//		// Search Pattern: Lines starting with: spring.datasource OR spring.jpa
	//		String regex = "^\\s*(spring\\.datasource|spring\\.jpa).*$";
	//
	//		List<MatchLocation> results = contentSearcher.findPropertiesMatches(applicationPath, "properties", regex);
	//
	//		// Expect: spring.datasource (Lines 5 - 8), spring.jpa (Lines 11-13)
	//		assertThat(results).hasSize(7);
	//
	//		// Verify line numbers and content
	//		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8, 11, 12, 13);
	//
	//		assertThat(results).extracting(MatchLocation::lineContent).contains(
	//				"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
	//				"spring.datasource.password=root", "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
	//				"spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=true",
	//				"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect");
	//	}

	//	@Test
	//	@DisplayName("Should find spring properties using recursive search")
	//	void testSearchForMigrationProperties() {
	//		String regex = "^\\s*spring\\.[a-zA-Z0-9\\.\\-]+=.*$";
	//
	//		List<MatchLocation> results = contentSearcher.findPropertiesMatches(applicationPath, "properties", regex);
	//
	//		assertThat(results).hasSize(7);
	//		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8, 11, 12, 13);
	//	}

}