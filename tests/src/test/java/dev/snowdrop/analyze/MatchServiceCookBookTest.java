package dev.snowdrop.analyze;

import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.CodeScannerService;
import dev.snowdrop.analyze.services.ScanCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.junit.jupiter.api.Assertions.*;

class MatchServiceCookBookTest {

    private CodeScannerService codeScannerService;
    private Config config;

    @TempDir
    Path tempDir;

    Path rulesPath;

    @BeforeEach
    void setUp() throws Exception {
        String applicationToScan = "spring-boot-todo-app";
        Path destinationPath = tempDir.resolve(applicationToScan);
        copyFolder(applicationToScan, destinationPath);

        String cookBook = "cookbook";
        rulesPath = tempDir.resolve( cookBook);
        copyFolder(cookBook, rulesPath);

        config = createTestConfig(destinationPath, rulesPath);

        ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
        codeScannerService = new CodeScannerService(config, scanCommandExecutor);
    }

    // ============================================
    // Tests based on resources/cookbook yaml
    // ============================================

    @ParameterizedTest
    @CsvSource({"quarkus/000-springboot-annotation-notfound.yaml,spring-boot-todo-app"})
    void testRule000_SpringBootAnnotationNotFound(String ruleSubPath, String appName) throws IOException {
        // Given a path of rules, got the rule to be processed
        List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(),ruleSubPath));

        // When
        Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("000-springboot-annotation-notfound"));

        Path rewriteYml = tempDir.resolve(Path.of(appName, "rewrite.yml"));
        assertTrue(Files.exists(rewriteYml));
    }

    @ParameterizedTest
    @CsvSource({"quarkus/001-springboot-replace-bom-quarkus.yaml,spring-boot-todo-app"})
    void testRule001_ReplaceBomQuarkus(String ruleSubPath, String appName) throws IOException {
        // Given
        List<Rule> rules = parseRulesFromFile(Path.of(rulesPath.toString(),ruleSubPath));

        // When
        Map<String, List<Match>> result = codeScannerService.scan(rules.getFirst()).getMatches();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("001-springboot-replace-bom-quarkus"));
        assertEquals(1, rules.getFirst().order());
        assertEquals("mandatory", rules.getFirst().category());
    }

    @Test
    void testRule002_AddQuarkusClass() {
        // Given
        Rule rule = createRule002_AddQuarkusClass();

        // When
        Map<String, List<Match>> result = codeScannerService.scan(rule).getMatches();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("002-springboot-add-class-quarkus"));
        assertEquals(2, rule.order());
    }

    @Test
    void testRule003_QuarkusMainAnnotation() {
        // Given
        Rule rule = createRule003_QuarkusMainAnnotation();

        // When
        Map<String, List<Match>> result = codeScannerService.scan(rule).getMatches();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("003-springboot-to-quarkusmain-annotation"));
        assertEquals(3, rule.order());
    }

    @Test
    void testRule004_RestAnnotations_WithOrConditions() {
        // Given
        Rule rule = createRule004_RestAnnotations();

        // When
        Map<String, List<Match>> result = codeScannerService.scan(rule).getMatches();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("004-springboot-to-quarkus-rest-annotations"));
        assertEquals(4, rule.order());
    }

    @ParameterizedTest
    @MethodSource("provideRealWorldRules")
    void testExecuteRewriteCmd_WithRealWorldRules(String ruleId, Rule rule, String expectedConditionType) {
        // When
        Map<String, List<Match>> result = codeScannerService.scan(rule).getMatches();

        // Then
        assertNotNull(result, "Result should not be null for rule: " + ruleId);
        assertTrue(result.containsKey(ruleId), "Result should contain key: " + ruleId);

        // Verify YAML file creation
        Path rewriteYml = tempDir.resolve("rewrite.yml");
        assertTrue(Files.exists(rewriteYml), "rewrite.yml should exist for rule: " + ruleId);
    }

    @Test
    void testExecuteRewriteCmd_WithDependencyCondition() throws IOException {
        // Given
        Rule rule = createRule001_ReplaceBomQuarkus();

        // When
        codeScannerService.scan(rule);

        // Then
        Path rewriteYml = tempDir.resolve("rewrite.yml");
        String content = Files.readString(rewriteYml);

        assertTrue(content.contains("dev.snowdrop.openrewrite.MatchConditions"));
        assertFalse(content.isEmpty());
    }

    @Test
    void testExecuteRewriteCmd_WithComplexOrConditions() throws IOException {
        // Given
        Rule rule = createRule004_RestAnnotations();

        // When
        codeScannerService.scan(rule);

        // Then
        Path rewriteYml = tempDir.resolve("rewrite.yml");
        assertTrue(Files.exists(rewriteYml));

        String content = Files.readString(rewriteYml);
        assertTrue(content.contains("specs.openrewrite.org/v1beta/recipe"));
        assertTrue(content.contains("recipeList:"));
    }

    @Test
    void testMultipleRulesInSequence() {
        // Given - Simulate sequential execution of Rules according to order
        Rule rule1 = createRule001_ReplaceBomQuarkus(); // order: 1
        Rule rule2 = createRule002_AddQuarkusClass(); // order: 2
        Rule rule3 = createRule003_QuarkusMainAnnotation(); // order: 3

        // When
        Map<String, List<Match>> result1 = codeScannerService.scan(rule1).getMatches();
        Map<String, List<Match>> result2 = codeScannerService.scan(rule2).getMatches();
        Map<String, List<Match>> result3 = codeScannerService.scan(rule3).getMatches();

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        assertTrue(rule1.order() < rule2.order());
        assertTrue(rule2.order() < rule3.order());
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

    // ============================================
    // Create actual rules based on cookbook/rules
    // ============================================

    /**
     * Rule 000: Search non existent annotation (dummy.SpringApplication)
     */
    public static Rule createRule000_AnnotationNotFound() {
        Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
            "java.annotation is 'dummy.SpringApplication'");

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
            "pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-web')");

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
            new String[]{"dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT",
                "org.openrewrite:rewrite-maven:8.62.4"})};

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
            "java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'");

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
            new String[]{"dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT",
                "org.openrewrite:rewrite-maven:8.62.4"})};

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
            "java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'");

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
            new String[]{"dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT",
                "org.openrewrite:rewrite-maven:8.62.4"})};

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
        // Condición compleja con múltiples OR
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

        Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(), complexCondition);

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
            new String[]{"dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT",
                "org.openrewrite:rewrite-maven:8.62.4"})};

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
        MatchServiceCookBookTest testInstance = new MatchServiceCookBookTest();
        testInstance.tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "test-" + System.currentTimeMillis());
        testInstance.config = testInstance.createTestConfig(testInstance.tempDir,testInstance.rulesPath);
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

    private static void copyFolder(String source, Path target, CopyOption... options) throws Exception {
        URL resourceUrl = MatchServiceCookBookTest.class.getClassLoader().getResource(source);
        if (resourceUrl == null) {
            throw new RuntimeException("Resource folder not found: " + source);
        }
        Path sourcePath = Paths.get(resourceUrl.toURI());
        Path destinationPath = target;

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
                Files.createDirectories(destinationPath.resolve(sourcePath.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                Files.copy(file, destinationPath.resolve(sourcePath.relativize(file).toString()), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Config createTestConfig(Path applicationToScan, Path rulesPath) {
        return new Config(applicationToScan.toString(), rulesPath, "springboot", "quarkus", "./jdt/konveyor-jdtls",
            "./jdt", "io.konveyor.tackle.ruleEntry", false, "json", "openrewrite");
    }

}