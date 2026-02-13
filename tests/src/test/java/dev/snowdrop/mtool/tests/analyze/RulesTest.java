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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static dev.snowdrop.mtool.analyze.utils.YamlRuleParser.parseRulesFromFolder;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest extends BaseRulesTest {

	private CodeScannerService codeScannerService;
	private Config config;

	@TempDir
	Path tempDir;

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

		// Configure the test with the parameters
		config = createTestConfig(destinationPath, rulesPath);

		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@Test
	void testMultipleRulesInSequence() throws IOException {
		// Given - Simulate sequential execution of Rules according to order: 000 to 004
		List<Rule> rules = parseRulesFromFolder(Path.of(rulesPath.toString(), "quarkus"));

		// When
		Map<String, List<Match>> result1 = codeScannerService.scan(rules.get(0)).getMatches();
		Map<String, List<Match>> result2 = codeScannerService.scan(rules.get(1)).getMatches();
		Map<String, List<Match>> result3 = codeScannerService.scan(rules.get(2)).getMatches();

		// Then
		assertNotNull(result1);
		assertNotNull(result2);
		assertNotNull(result3);

		assertTrue(rules.get(0).order() < rules.get(1).order());
		assertTrue(rules.get(2).order() < rules.get(3).order());
	}

	@Test
	void testRuleCategories() {
		// Given
		Rule mandatoryRule = createRule001_ReplaceBomQuarkus();
		Rule optionalRule = createRule000_AnnotationNotFound();

		// Then
		assertEquals("mandatory", mandatoryRule.category());
		assertEquals("optional", optionalRule.category());
	}

	@Test
	void testRuleLabels() {
		// Given
		Rule rule = createRule001_ReplaceBomQuarkus();

		// Then
		assertTrue(rule.labels().contains("konveyor.io/source=springboot"));
		assertTrue(rule.labels().contains("konveyor.io/target=quarkus"));
	}

	@ParameterizedTest
	@MethodSource("provideRealWorldRules")
	void testExecuteRewriteCmd_WithRealWorldRules(String ruleId, Rule rule, String expectedConditionType) {
		// When
		Map<String, List<Match>> result = codeScannerService.scan(rule).getMatches();

		// Then
		assertNotNull(result, "Result should not be null for rule: " + ruleId);
		assertTrue(result.containsKey(ruleId), "Result should contain key: " + ruleId);
	}

	// ============================================
	// Create actual rules based on cookbook/rules
	// ============================================

	/**
	 * Rule 000: Search non-existent annotation (dummy.SpringApplication)
	 */
	public static Rule createRule000_AnnotationNotFound() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'dummy.SpringApplication'", null);

		Rule.Instruction instructions = new Rule.Instruction(null, // no AI instructions
				null, // no manual instructions
				null // no openrewrite instructions
		);

		return new Rule("optional", Collections.emptyList(), "Remove the SpringBoot @SpringBootApplication annotation",
				1, List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(),
				"The dummy.SpringApplication annotation do not exist", "000-springboot-annotation-notfound", null, when,
				Collections.emptyList(), 0, // no order specified in YAML
				instructions);
	}

	/**
	 * Rule 001: Replace SpringBoot BOM with Quarkus
	 */
	private Rule createRule001_ReplaceBomQuarkus() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-web')", null);

		// AI Instructions
		Rule.Ai[] aiInstructions = new Rule.Ai[]{new Rule.Ai(null,
				// promptMessage (deprecated en favor de tasks)
				Arrays.asList(
						"Add to the pom.xml file the Quarkus BOM dependency within the dependencyManagement section and the following dependencies: quarkus-arc, quarkus-core",
						"The version of quarkus to be used and to included within the pom.xml properties is 3.26.4."))};

		// Manual Instructions
		Rule.Manual[] manualInstructions = new Rule.Manual[]{new Rule.Manual(
				"Add the Quarkus BOM and quarkus-arc, quarkus-core dependencies within the pom.xml file")};

		// OpenRewrite Instructions
		Rule.Openrewrite[] openrewriteInstructions = new Rule.Openrewrite[]{new Rule.Openrewrite(
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file",
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file.", null, // preconditions
				null, // recipeList (aquí podrías añadir los recipes si los necesitas)
				new String[]{"dev.snowdrop.mtool:openrewrite-recipes:1.0.0-SNAPSHOT",
						"org.openrewrite:rewrite-maven:8.73.0"})};

		Rule.Instruction instructions = new Rule.Instruction(aiInstructions, manualInstructions,
				openrewriteInstructions);

		return new Rule("mandatory", Collections.emptyList(), "SpringBoot to Quarkus", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(),
				"SpringBoot to Quarkus.", "001-springboot-replace-bom-quarkus", null, when, Collections.emptyList(), 1,
				instructions);
	}

	/**
	 * Rule 002: Add Quarkus class
	 */
	private Rule createRule002_AddQuarkusClass() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'", null);

		List<String> aiTasks = List.of(
				"Add a new class com.todo.app.TodoApplication which implements QuarkusApplication.",
				"Don't add the annotation @QuarkusMain to this class.",
				"Use stdout to send a message: Hello user using args[0] within the method which override run().");

		// AI Instructions
		Rule.Ai[] aiInstructions = new Rule.Ai[]{new Rule.Ai(null,
				// promptMessage (deprecated en favor de tasks)
				aiTasks)};

		// Manual Instructions
		Rule.Manual[] manualInstructions = new Rule.Manual[]{new Rule.Manual("See openrewrite instructions")};

		// OpenRewrite Instructions
		Rule.Openrewrite[] openrewriteInstructions = new Rule.Openrewrite[]{new Rule.Openrewrite(
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file",
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file.", null, // preconditions
				null, // recipeList (aquí podrías añadir los recipes si los necesitas)
				new String[]{"dev.snowdrop.mtool:openrewrite-recipes:1.0.0-SNAPSHOT",
						"org.openrewrite:rewrite-maven:8.73.0"})};

		Rule.Instruction instructions = new Rule.Instruction(aiInstructions, manualInstructions,
				openrewriteInstructions);

		return new Rule("mandatory", Collections.emptyList(), "SpringBoot to Quarkus", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(),
				"SpringBoot to Quarkus.", "002-springboot-add-class-quarkus", null, when, Collections.emptyList(), 2,
				instructions);
	}

	/**
	 * Rule 003: Add @QuarkusMain annotation
	 */
	private Rule createRule003_QuarkusMainAnnotation() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'", null);

		List<String> aiTasks = List.of(
				"Can you remove the @SpringBootApplication from the java file com.todo.app.AppApplication.java.",
				"Next add to the same java class the @QuarkusMain annotation.",
				"The AppApplication should not implement QuarkusApplication.",
				"Pass as first argument: TodoApplication.class to the Quarkus.run() method");

		// AI Instructions
		Rule.Ai[] aiInstructions = new Rule.Ai[]{new Rule.Ai(null,
				// promptMessage (deprecated en favor de tasks)
				aiTasks)};

		// Manual Instructions
		Rule.Manual[] manualInstructions = new Rule.Manual[]{new Rule.Manual("See openrewrite instructions")};

		// OpenRewrite Instructions
		Rule.Openrewrite[] openrewriteInstructions = new Rule.Openrewrite[]{new Rule.Openrewrite(
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file",
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file.", null, // preconditions
				null, // recipeList (aquí podrías añadir los recipes si los necesitas)
				new String[]{"dev.snowdrop.mtool:openrewrite-recipes:1.0.0-SNAPSHOT",
						"org.openrewrite:rewrite-maven:8.73.0"})};

		Rule.Instruction instructions = new Rule.Instruction(aiInstructions, manualInstructions,
				openrewriteInstructions);

		return new Rule("mandatory", Collections.emptyList(), "SpringBoot to Quarkus", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(),
				"SpringBoot to Quarkus.", "003-springboot-to-quarkusmain-annotation", null, when,
				Collections.emptyList(), 3, instructions);
	}

	/**
	 * Rule 004: Replace REST annotations (multiple OR)
	 */
	private Rule createRule004_RestAnnotations() {
		// Complex condition with multiple OR
		String complexCondition = """
				java.annotation is 'org.springframework.stereotype.Controller' OR
				java.annotation is 'org.springframework.beans.factory.annotation.Autowired' OR
				java.annotation is 'org.springframework.web.bind.annotation.GetMapping' OR
				java.annotation is 'org.springframework.web.bind.annotation.DeleteMapping' OR
				java.annotation is 'org.springframework.web.bind.annotation.PathVariable' OR
				java.annotation is 'org.springframework.web.bind.annotation.PostMapping' OR
				java.annotation is 'org.springframework.web.bind.annotation.RequestBody' OR
				java.annotation is 'org.springframework.web.bind.annotation.ResponseBody'
				""".trim();

		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(), complexCondition, null);

		// AI Instructions
		Rule.Ai[] aiInstructions = new Rule.Ai[]{new Rule.Ai(null,
				// promptMessage (deprecated en favor de tasks)
				List.of("TODO"))};

		// Manual Instructions
		Rule.Manual[] manualInstructions = new Rule.Manual[]{new Rule.Manual("See openrewrite instructions")};

		// OpenRewrite Instructions
		Rule.Openrewrite[] openrewriteInstructions = new Rule.Openrewrite[]{new Rule.Openrewrite(
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file",
				"Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file.", null, // preconditions
				null, // recipeList (aquí podrías añadir los recipes si los necesitas)
				new String[]{"dev.snowdrop.mtool:openrewrite-recipes:1.0.0-SNAPSHOT",
						"org.openrewrite:rewrite-maven:8.73.0"})};

		Rule.Instruction instructions = new Rule.Instruction(aiInstructions, manualInstructions,
				openrewriteInstructions);

		return new Rule("mandatory", Collections.emptyList(), "SpringBoot to Quarkus", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(),
				"SpringBoot to Quarkus.", "004-springboot-to-quarkus-rest-annotations", null, when,
				Collections.emptyList(), 4, instructions);
	}

	// ============================================
	// Provider for parametrized tests
	// ============================================
	private static Stream<Arguments> provideRealWorldRules() {
		RulesTest testInstance = new RulesTest();
		testInstance.tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "test-" + System.currentTimeMillis());
		testInstance.config = testInstance.createTestConfig(testInstance.tempDir, testInstance.rulesPath);
		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		testInstance.codeScannerService = new CodeScannerService(testInstance.config, scanCommandExecutor);

		return Stream.of(
				Arguments.of("000-springboot-annotation-notfound", testInstance.createRule000_AnnotationNotFound(),
						"simple"),
				Arguments.of("001-springboot-replace-bom-quarkus", testInstance.createRule001_ReplaceBomQuarkus(),
						"dependency"),
				Arguments.of("002-springboot-add-class-quarkus", testInstance.createRule002_AddQuarkusClass(),
						"annotation"),
				Arguments.of("003-springboot-to-quarkusmain-annotation",
						testInstance.createRule003_QuarkusMainAnnotation(), "annotation"),
				Arguments.of("004-springboot-to-quarkus-rest-annotations", testInstance.createRule004_RestAnnotations(),
						"or-conditions"));
	}
}