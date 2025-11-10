package dev.snowdrop.parser;

import dev.snowdrop.model.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;
import java.util.Set;

public class QueryWithAndOperatorTest extends AbstractQueryParser {

	@Test
	public void queryWithClauseAnnotationAndClauseAnnotation() {
		String queryWithAnd = "java.annotation is \"@SpringBootApplication\" AND java.annotation is \"@ResponseBody\"";
		QueryVisitor visitor = parseQuery(queryWithAnd);

		// Don't include simple quotes around the key or value
		Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
		Query queryB = new Query("java", "annotation", Map.of("name", "@ResponseBody"));

		Set<Query> simpleQueries = visitor.getSimpleQueries();
		var queryList = simpleQueries.stream().toList();
		Assert.assertTrue(queryList.isEmpty());

		Set<Query> orQueries = visitor.getOrQueries();
		queryList = orQueries.stream().toList();
		Assert.assertTrue(queryList.isEmpty());

		Set<Query> andQueries = visitor.getAndQueries();
		queryList = andQueries.stream().toList();
		Assert.assertTrue(queryList.size() == 2);
		Assertions.assertEquals(queryList.get(0), queryA);
		Assertions.assertEquals(queryList.get(1), queryB);
	}
	@Test
	public void queryWithClauseAnnotationAndClausePomDependency() {
		String queryWithAnd = "java.annotation is \"@SpringBootApplication\" AND pom.dependency is (artifactId='quarkus-core', version='3.16.2')";
		QueryVisitor visitor = parseQuery(queryWithAnd);

		// Don't include simple quotes around the key or value
		Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
		Query queryB = new Query("pom", "dependency", Map.of("artifactId", "quarkus-core", "version", "3.16.2"));

		Set<Query> simpleQueries = visitor.getSimpleQueries();
		var queryList = simpleQueries.stream().toList();
		Assert.assertTrue(queryList.isEmpty());

		Set<Query> orQueries = visitor.getOrQueries();
		queryList = orQueries.stream().toList();
		Assert.assertTrue(queryList.isEmpty());

		Set<Query> queries = visitor.getAndQueries();
		queryList = queries.stream().toList();
		Assert.assertTrue(queryList.size() == 2);
		Assertions.assertEquals(queryList.get(0), queryA);
		Assertions.assertEquals(queryList.get(1), queryB);
	}

	@Test
	public void testMixedQuoteSyntax() {
		String queryWithMixedQuotes = "java.annotation is '@SpringBootApplication' AND pom.dependency is \"quarkus-core\"";
		QueryVisitor visitor = parseQuery(queryWithMixedQuotes);

		Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
		Query queryB = new Query("pom", "dependency", Map.of("artifactId", "quarkus-core"));

		Set<Query> queries = visitor.getAndQueries();
		var queryList = queries.stream().toList();
		Assert.assertTrue(queryList.size() == 2);
		Assertions.assertTrue(queries.contains(queryA));
		Assertions.assertTrue(queries.contains(queryB));
	}

	@Test
	public void testDoubleQuotesInKeyValuePairs() {
		String queryWithDoubleQuotes = "java.annotation is (name=\"@SpringBootApplication\") AND pom.dependency is (artifactId=\"quarkus-core\", version=\"3.16.2\")";
		QueryVisitor visitor = parseQuery(queryWithDoubleQuotes);

		Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
		Query queryB = new Query("pom", "dependency", Map.of("artifactId", "quarkus-core", "version", "3.16.2"));

		Set<Query> queries = visitor.getAndQueries();
		var queryList = queries.stream().toList();
		Assert.assertTrue(queryList.size() == 2);
		Assertions.assertTrue(queries.contains(queryA));
		Assertions.assertTrue(queries.contains(queryB));
	}
}
