package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.when;

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
				"java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'");
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"simple-condition-rule", null, when, Collections.emptyList(), 1, null);

		QueryVisitor queryVisitor = QueryUtils.parseAndVisit(rule.when().condition());
		List<Rewrite> rewrites = List.of(new Rewrite("9a02b8b2-2148-48ea-b29d-6d2f1aa223a5",
				"2025-11-10_16-11-55-417/dev.snowdrop.openrewrite.java.table.AnnotationsReport.csv:5|JAVA.ANNOTATION|org.springframework.boot.autoconfigure.SpringBootApplication"));
		when(scanCommandExecutor.executeQueryCommand(config, queryVisitor.getSimpleQueries())).thenReturn(rewrites);

		ScanningResult scanningResult = codeScannerService.scan(rule);
		assertTrue(scanningResult.isMatchSucceeded());
		Map<String, List<Rewrite>> result = scanningResult.getRewrites();
		assertNotNull(result);
		assertTrue(result.containsKey("simple-condition-rule"));
		assertEquals(1, result.get("simple-condition-rule").size());
		assertThat(result.get("simple-condition-rule")).isEqualTo(rewrites);

	}

	// ---------------------------------------------------------------------
	// CASE 2: Multiple OR queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmdWithOrCondition() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"java.annotation is 'org.springframework.stereotype.Controller' OR\n"
						+ "      java.annotation is 'org.springframework.beans.factory.annotation.Autowired' OR\n"
						+ "      java.annotation is 'org.springframework.web.bind.annotation.GetMapping'");
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"or-condition-test", null, when, Collections.emptyList(), 1, null);

		Map<String, List<Rewrite>> result = codeScannerService.scan(rule).getRewrites();
		assertNotNull(result);
		assertTrue(result.containsKey("or-condition-test"));

	}

	// ---------------------------------------------------------------------
	// CASE 3: Multiple AND queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmdWithAndCondition() {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(),
				"pom.dependency is spring-boot AND java.annotation is 'RestController'");
		Rule rule = new Rule("mandatory", Collections.emptyList(), "desc", 1,
				List.of("konveyor.io/source=springboot", "konveyor.io/target=quarkus"), Collections.emptyList(), "help",
				"and-condition-test", null, when, Collections.emptyList(), 1, null);

		Map<String, List<Rewrite>> result = codeScannerService.scan(rule).getRewrites();
		assertNotNull(result);
		assertTrue(result.containsKey("and-condition-test"));

	}

	// ---------------------------------------------------------------------
	// CASE 5: findRecordsMatching - directory does not exist
	// ---------------------------------------------------------------------
	@Test
	void testFindRecordsMatching_NoDirectory() {
		List<Rewrite> results = callFindRecordsMatching("non-existent", "match123");
		assertTrue(results.isEmpty());
	}

	// ---------------------------------------------------------------------
	// CASE 6: findRecordsMatching - empty directory (Openrewrite)
	// ---------------------------------------------------------------------
	@Test
	void testFindRecordsMatching_EmptyDirectory() throws IOException {
		Path dir = tempDir.resolve("target/rewrite/datatables");
		Files.createDirectories(dir);

		List<Rewrite> results = callFindRecordsMatching(tempDir.toString(), "match123");
		assertTrue(results.isEmpty());
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------
	private List<Rewrite> callFindRecordsMatching(String path, String matchId) {
		try {
			var method = CodeScannerService.class.getDeclaredMethod("findRecordsMatching", String.class, String.class);
			method.setAccessible(true);
			return (List<Rewrite>) method.invoke(codeScannerService, path, matchId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
