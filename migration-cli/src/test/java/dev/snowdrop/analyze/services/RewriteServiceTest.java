package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.model.Query;
import dev.snowdrop.parser.QueryVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RewriteService internal paths and edge cases.
 */
class RewriteServiceTest {

	private Config config;
	private RewriteService rewriteService;
	private Path tempDir;

	@BeforeEach
	void setup() throws IOException {
		tempDir = Files.createTempDirectory("rewrite-test");
		config = new Config(tempDir.toString(), Paths.get("./rules"), "springboot", "quarkus", "./jdt/konveyor-jdtls",
				"./jdt", "io.konveyor.tackle.ruleEntry", false, "json", "openrewrite");
		rewriteService = new RewriteService(config);
	}

	@AfterEach
	void cleanup() throws IOException {
		Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
	}

	// ---------------------------------------------------------------------
	// CASE 1: Condition simple (visitor.getSimpleQueries().size() == 1)
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmd_SimpleCondition() {
		QueryVisitor mockVisitor = mock(QueryVisitor.class);
		when(mockVisitor.getSimpleQueries())
				.thenReturn(Set.of(new Query("java.annotation", "is", Map.of("name", "@SpringBootApplication"))));
		when(mockVisitor.getOrQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getAndQueries()).thenReturn(Collections.emptySet());

		Rule rule = mockRule("simple-test", "java.annotation is '@SpringBootApp'");

		try (MockedStatic<dev.snowdrop.parser.QueryUtils> mocked = mockStatic(dev.snowdrop.parser.QueryUtils.class)) {
			mocked.when(() -> dev.snowdrop.parser.QueryUtils.parseAndVisit(anyString())).thenReturn(mockVisitor);

			Map<String, List<Rewrite>> result = rewriteService.executeRewriteCmd(rule);
			assertNotNull(result);
			assertTrue(result.containsKey("simple-test"));
		}
	}

	// ---------------------------------------------------------------------
	// CASE 2: Multiple OR queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmd_OrCondition() {
		QueryVisitor mockVisitor = mock(QueryVisitor.class);
		when(mockVisitor.getSimpleQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getOrQueries())
				.thenReturn(Set.of(new Query("java.annotation", "is", Map.of("name", "@RestController")),
						new Query("java.annotation", "is", Map.of("name", "@GetMapping"))));
		when(mockVisitor.getAndQueries()).thenReturn(Collections.emptySet());

		Rule rule = mockRule("or-condition-test",
				"java.annotation is '@RestController' OR java.annotation is '@GetMapping'");

		try (MockedStatic<dev.snowdrop.parser.QueryUtils> mocked = mockStatic(dev.snowdrop.parser.QueryUtils.class)) {
			mocked.when(() -> dev.snowdrop.parser.QueryUtils.parseAndVisit(anyString())).thenReturn(mockVisitor);

			Map<String, List<Rewrite>> result = rewriteService.executeRewriteCmd(rule);
			assertNotNull(result);
			assertTrue(result.containsKey("or-condition-test"));
		}
	}

	// ---------------------------------------------------------------------
	// CASE 3: Multiple AND queries
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmd_AndCondition() {
		QueryVisitor mockVisitor = mock(QueryVisitor.class);
		when(mockVisitor.getSimpleQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getOrQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getAndQueries())
				.thenReturn(Set.of(new Query("pom.dependency", "is", Map.of("name", "spring-boot")),
						new Query("java.annotation", "is", Map.of("name", "@SpringBootApp"))));

		Rule rule = mockRule("and-condition-test",
				"pom.dependency is 'spring-boot' AND java.annotation is '@SpringBootApp'");

		try (MockedStatic<dev.snowdrop.parser.QueryUtils> mocked = mockStatic(dev.snowdrop.parser.QueryUtils.class)) {
			mocked.when(() -> dev.snowdrop.parser.QueryUtils.parseAndVisit(anyString())).thenReturn(mockVisitor);

			Map<String, List<Rewrite>> result = rewriteService.executeRewriteCmd(rule);
			assertNotNull(result);
			assertTrue(result.containsKey("and-condition-test"));
		}
	}

	// ---------------------------------------------------------------------
	// CASE 4: No valid condition
	// ---------------------------------------------------------------------
	@Test
	void testExecuteRewriteCmd_NoValidCondition() {
		QueryVisitor mockVisitor = mock(QueryVisitor.class);
		when(mockVisitor.getSimpleQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getOrQueries()).thenReturn(Collections.emptySet());
		when(mockVisitor.getAndQueries()).thenReturn(Collections.emptySet());

		Rule rule = mockRule("empty-condition", "java.annotation is 'none'");

		try (MockedStatic<dev.snowdrop.parser.QueryUtils> mocked = mockStatic(dev.snowdrop.parser.QueryUtils.class)) {
			mocked.when(() -> dev.snowdrop.parser.QueryUtils.parseAndVisit(anyString())).thenReturn(mockVisitor);

			Map<String, List<Rewrite>> result = rewriteService.executeRewriteCmd(rule);
			assertTrue(result.containsKey("empty-condition"));
			assertTrue(result.get("empty-condition").isEmpty());
		}
	}

	// ---------------------------------------------------------------------
	// CASE 6: findRecordsMatching - directory does not exist
	// ---------------------------------------------------------------------
	@Test
	void testFindRecordsMatching_NoDirectory() {
		List<Rewrite> results = callFindRecordsMatching("non-existent", "match123");
		assertTrue(results.isEmpty());
	}

	// ---------------------------------------------------------------------
	// CASE 7: findRecordsMatching - empty directory (Openrewrite)
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
	private Rule mockRule(String id, String condition) {
		Rule.When when = new Rule.When(null, Collections.emptyList(), Collections.emptyList(), condition);
		return new Rule("mandatory", Collections.emptyList(), "desc", 1, List.of("konveyor.io/source=springboot"),
				Collections.emptyList(), "help", id, null, when, Collections.emptyList(), 1, null);
	}

	private List<Rewrite> callFindRecordsMatching(String path, String matchId) {
		try {
			var method = RewriteService.class.getDeclaredMethod("findRecordsMatching", String.class, String.class);
			method.setAccessible(true);
			return (List<Rewrite>) method.invoke(rewriteService, path, matchId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
