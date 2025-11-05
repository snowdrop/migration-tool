package dev.snowdrop.parser;

import dev.snowdrop.model.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;
import java.util.Set;

public class SimpleQueryTest extends AbstractQueryParser {
	@Test
	public void clauseWithSingleQuotes() {
		String simpleQuery = "java.annotation is '@SpringBootApplication'";
		QueryVisitor visitor = parseQuery(simpleQuery);

		// Don't include simple quotes around the key or value
		Query query = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		Set<Query> queries = visitor.getSimpleQueries();
		Assert.assertTrue(queries.size() == 1);
		Assertions.assertEquals(queries.stream().findFirst().get(), query);
	}

	@Test
	public void clauseWithoutKeyValuePairs() {
		String simpleQuery = "java.annotation is '@SpringBootApplication'";
		QueryVisitor visitor = parseQuery(simpleQuery);

		// Should automatically use "name" as default key for annotation
		Query expectedQuery = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		Set<Query> queries = visitor.getSimpleQueries();
		Assert.assertTrue(queries.size() == 1);
		Assertions.assertTrue(queries.contains(expectedQuery));
	}

	@Test
	public void clauseWithDoubleQuotes() {
		String annotationQuery = "java.annotation is \"@SpringBootApplication\"";
		QueryVisitor visitor = parseQuery(annotationQuery);

		// Should automatically use "name" as default key for annotation
		Query expectedQuery = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		Set<Query> queries = visitor.getSimpleQueries();
		Assert.assertTrue(queries.size() == 1);
		Assertions.assertTrue(queries.contains(expectedQuery));
	}

	@Test
	public void shouldParseAPomDependencyQuery() {
		String annotationQuery = "pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-web')";
		QueryVisitor visitor = parseQuery(annotationQuery);
		Query expectedQuery = new Query("pom", "dependency",
				Map.of("gavs", "org.springframework.boot:spring-boot-starter-web"));

		Set<Query> queries = visitor.getSimpleQueries();
		Assert.assertTrue(queries.size() == 1);
		Assertions.assertTrue(queries.contains(expectedQuery));
	}
}
