package dev.snowdrop.mtool.tests.analyze;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.scanner.CodeScannerService;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFolder;
import static org.junit.jupiter.api.Assertions.*;

class MatchServiceCookBookTest extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	@TempDir
	Path tempDir;

	String jdtls;

	@BeforeEach
	void setUp() throws Exception {
		// Copy the code of the project to analyze within the temp dir
		String applicationToScan = "spring-boot-todo-app";
		Path destinationPath = tempDir.resolve(applicationToScan);
		copyFolder(applicationToScan, destinationPath);

		// Copy the rules to be evaluated the temp dir
		String cookBook = "cookbook";
		rulesPath = tempDir.resolve(cookBook);
		copyFolder(cookBook, rulesPath);

		// Copy the jdt-ls server
		String jdtls = "jdt/konveyor-jdtls";
		copyFolder(jdtls, tempDir.resolve(jdtls));

		// Configure the test with the parameters
		config = createTestConfig(destinationPath, rulesPath, jdtls);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	// ============================================
	// Tests based on resources/cookbook yaml
	// ============================================

	@ParameterizedTest
	@CsvSource({"quarkus/000-springboot-annotation-notfound.yaml,spring-boot-todo-app"})
	void testRule000_SpringBootAnnotationNotFound(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testRule000_SpringBootAnnotationNotFound");
		// Given a path of rules, got the rule to be processed
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));
		rules.forEach(r -> System.out.println("### rule: " + r.when().condition()));

		// When
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		// The recipe has been executed as we got a result having a key but the result is empty as no match succeeded to search about the annotation
		assertNotNull(result);
		assertTrue(result.containsKey("000-springboot-annotation-notfound"));

		List<Match> matches = result.get("000-springboot-annotation-notfound");
		assertEquals(0, matches.size());
		System.out.println("########################################");
	}

	@ParameterizedTest
	@CsvSource({"quarkus/001-springboot-replace-bom-quarkus.yaml,spring-boot-todo-app"})
	void testRule001_ReplaceBomQuarkus(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testRule001_ReplaceBomQuarkus");

		// We are searching about the following annotation:
		// java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Against the project: spring-boot-todo-app
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		assertEquals(1, rules.getFirst().order());
		assertEquals("mandatory", rules.getFirst().category());

		// We should get a match against the following Java class: src/main/java/com/todo/app/AppApplication.java
		assertNotNull(result);
		assertTrue(result.containsKey("001-springboot-replace-bom-quarkus"));
		assertEquals(1, result.get("001-springboot-replace-bom-quarkus").size());
		System.out.println("########################################");
	}

	@ParameterizedTest
	@CsvSource({"quarkus/002-springboot-add-class-quarkus.yaml,spring-boot-todo-app"})
	void testRule002_AddQuarkusClass(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testRule002_AddQuarkusClass");

		// We are searching about the following annotation:
		// java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Against the project: spring-boot-todo-app
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		// We should get a match against the following Java class: src/main/java/com/todo/app/AppApplication.java
		assertNotNull(result);
		assertTrue(result.containsKey("002-springboot-add-class-quarkus"));
		assertEquals(1, result.get("002-springboot-add-class-quarkus").size());
		System.out.println("########################################");
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({"quarkus/003-springboot-to-quarkus-main-annotation.yaml,spring-boot-todo-app"})
	void testRule003_QuarkusMainAnnotation(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testRule003_QuarkusMainAnnotation");

		// We are searching about the following annotation:
		// java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Against the project: spring-boot-todo-app
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		// We should get a match against the following Java class: src/main/java/com/todo/app/AppApplication.java
		assertNotNull(result);
		assertTrue(result.containsKey("003-springboot-to-quarkus-main-annotation"));
		assertEquals(1, result.get("003-springboot-to-quarkus-main-annotation").size());
		System.out.println("########################################");
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({"quarkus/004-springboot-to-quarkus-rest-annotations.yaml,spring-boot-todo-app"})
	void testRule004_RestAnnotations_WithOrConditions(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testRule004_RestAnnotations_WithOrConditions");
		// Given
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// When
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("004-springboot-to-quarkus-rest-annotations"));
		assertEquals(4, rules.getFirst().order());
		System.out.println("########################################");
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({"quarkus/004-springboot-to-quarkus-rest-annotations.yaml,spring-boot-todo-app"})
	void testExecuteRewriteCmd_WithComplexOrConditions(String ruleSubPath, String appName) throws Exception {
		System.out.println("\n### Running test: testExecuteRewriteCmd_WithComplexOrConditions");

		/*
		 We are searching about the following annotations:
		 java.annotation is 'org.springframework.stereotype.Controller' OR
		 java.annotation is 'org.springframework.beans.factory.annotation.Autowired' OR
		 java.annotation is 'org.springframework.web.bind.annotation.GetMapping' OR
		 java.annotation is 'org.springframework.web.bind.annotation.DeleteMapping' OR
		 java.annotation is 'org.springframework.web.bind.annotation.PathVariable' OR
		 java.annotation is 'org.springframework.web.bind.annotation.PostMapping' OR
		 java.annotation is 'org.springframework.web.bind.annotation.RequestBody' OR
		 java.annotation is 'org.springframework.web.bind.annotation.ResponseBody'
		*/
		List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(), ruleSubPath));

		// Against the project: spring-boot-todo-app
		Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();
		runCat(Path.of(tempDir.toString(), appName, "target/rewrite/rewrite.patch"));

		// Then, we got as result
		assertNotNull(result);
		assertTrue(result.containsKey("004-springboot-to-quarkus-rest-annotations"));
		/*
		  We should get a match against the following Java classes
		  src/main/java/com/todo/app/controller/TaskController.java
		  src/main/java/com/todo/app/service/TaskServiceImpl.java
		 */
		System.out.println(
				"###### Match result size: " + result.get("004-springboot-to-quarkus-rest-annotations").size());
		assertTrue(result.get("004-springboot-to-quarkus-rest-annotations").size() == 13);
		System.out.println("########################################");
	}
}