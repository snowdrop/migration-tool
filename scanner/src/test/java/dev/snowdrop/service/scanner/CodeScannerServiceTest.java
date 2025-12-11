package dev.snowdrop.service.scanner;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.parser.Query;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeScannerServiceTest {

	private Config config;
	private CodeScannerService codeScannerService;
	private ScanCommandExecutor scanCommandExecutor;
	private Path tempDir;

	@BeforeEach
	void setup() throws IOException {
		tempDir = Files.createTempDirectory("rewrite-test");
		config = new Config(tempDir.toString(), Paths.get("./rules"), "springboot", "quarkus", "./jdt/konveyor-jdtls",
				"./jdt", "io.konveyor.tackle.ruleEntry", false, "json", "openrewrite");
		scanCommandExecutor = Mockito.mock(ScanCommandExecutor.class);
		codeScannerService = new CodeScannerService(config, scanCommandExecutor);
	}

	@AfterEach
	void cleanup() throws IOException {
		Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
	}

	// ---------------------------------------------------------------------
	// CASE 1: Condition simple (visitor.getSimpleQueries().size() == 1)
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmdWithSimpleCondition() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'", null);
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"simple-condition-rule", null, when, Collections.emptyList(), 1, null);

		QueryVisitor queryVisitor = QueryUtils.parseAndVisit(rule.when().condition());
		List<Match> matches = List.of(new Match("1", "openrewrite",
				"2025-11-10_16-11-55-417/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:5|JAVA.ANNOTATION|org.springframework.boot.autoconfigure.SpringBootApplication"));
		Mockito.when(
				scanCommandExecutor.executeCommandForQuery(config, queryVisitor.getSimpleQueries().iterator().next()))
				.thenReturn(matches);

		ScanningResult scanningResult = codeScannerService.scan(rule);
		Assertions.assertTrue(scanningResult.isMatchSucceeded());
		Map<String, List<Match>> result = scanningResult.getMatches();
		assertNotNull(result);
		assertTrue(result.containsKey("simple-condition-rule"));
		assertEquals(1, result.get("simple-condition-rule").size());
		assertThat(result.get("simple-condition-rule")).isEqualTo(matches);

	}

	// ---------------------------------------------------------------------
	// CASE 2: Multiple OR queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmdWithOrCondition() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'org.springframework.stereotype.Controller' OR\n"
						+ "      java.annotation is 'org.springframework.beans.factory.annotation.Autowired' OR\n"
						+ "      java.annotation is 'org.springframework.web.bind.annotation.GetMapping'",
				null);
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"or-condition-test", null, when, Collections.emptyList(), 1, null);

		QueryVisitor queryVisitor = QueryUtils.parseAndVisit(rule.when().condition());
		Match controller = new Match("1", "openrewrite",
				"2025-11-11_15-43-31-451/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:13|JAVA.ANNOTATION|org.springframework.stereotype.Controller");
		Match autowired = new Match("2", "openrewrite",
				"2025-11-11_15-43-31-451/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:8|JAVA.ANNOTATION|org.springframework.beans.factory.annotation.Autowired");
		Match getMapping = new Match("3", "openrewrite",
				"2025-11-11_15-43-31-451/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:14|JAVA.ANNOTATION|org.springframework.web.bind.annotation.GetMapping");
		List<Match> matches = List.of(controller, getMapping, autowired);
		Mockito.when(scanCommandExecutor.executeCommandForQuery(config, queryVisitor.getOrQueries().iterator().next()))
				.thenReturn(matches);

		ScanningResult scanningResult = codeScannerService.scan(rule);
		Assertions.assertTrue(scanningResult.isMatchSucceeded());
		Map<String, List<Match>> result = scanningResult.getMatches();
		assertNotNull(result);
		assertTrue(result.containsKey("or-condition-test"));
		assertEquals(3, result.get("or-condition-test").size());
		assertThat(result.get("or-condition-test")).isEqualTo(matches);

	}

	@Test
	void testExecuteRewriteCmdWithAndConditionNew() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'Autowired' AND java.annotation is 'RestController'", null);
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"and-condition-test", null, when, Collections.emptyList(), 1, null);

		Match controller = new Match("1", "openrewrite",
				"2025-11-11_15-43-31-451/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:13|JAVA.ANNOTATION|org.springframework.stereotype.RestController");
		Match autowired = new Match("2", "openrewrite",
				"2025-11-11_15-43-31-451/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:8|JAVA.ANNOTATION|org.springframework.beans.factory.annotation.Autowired");
		List<Match> matches = List.of(controller, autowired);
		Query queryAutowired = new Query("java", "annotation", Map.of("name", "Autowired"));
		Query queryController = new Query("java", "annotation", Map.of("name", "RestController"));
		Mockito.when(scanCommandExecutor.executeCommandForQuery(config, queryAutowired)).thenReturn(List.of(autowired));
		Mockito.when(scanCommandExecutor.executeCommandForQuery(config, queryController))
				.thenReturn(List.of(controller));

		ScanningResult scanningResult = codeScannerService.scan(rule);
		Assertions.assertTrue(scanningResult.isMatchSucceeded());
		Map<String, List<Match>> result = scanningResult.getMatches();
		assertNotNull(result);
		assertTrue(result.containsKey("and-condition-test"));
		assertEquals(2, result.get("and-condition-test").size());
		assertThat(result.get("and-condition-test")).containsExactlyInAnyOrderElementsOf(matches);

	}

	// ---------------------------------------------------------------------
	// CASE 3: Multiple AND queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmdWithAndCondition() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"pom.dependency is spring-boot AND java.annotation is 'RestController'", null);
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"and-condition-test", null, when, Collections.emptyList(), 1, null);

		ScanningResult scanningResult = codeScannerService.scan(rule);
		Assertions.assertFalse(scanningResult.isMatchSucceeded());
		Map<String, List<Match>> result = scanningResult.getMatches();
		assertNotNull(result);
		assertTrue(result.containsKey("and-condition-test"));

	}

}
