package dev.snowdrop.parser;

import dev.snowdrop.model.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;
import java.util.Set;

public class QueryParserTest extends AbstractQueryParser{
    @Test
    public void simpleQuery() {
        String simpleQuery = "java.annotation is \"@SpringBootApplication\"";
        QueryVisitor visitor = parseQuery(simpleQuery);

        // Don't include simple quotes around the key or value
        Query query = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertEquals(queries.stream().findFirst().get(), query);
    }

    @Test
    public void queryWithAnd() {
        String queryWithAnd = "java.annotation is \"@SpringBootApplication\" AND pom.dependency is (artifactId='quarkus-core', version='3.16.2')";
        QueryVisitor visitor = parseQuery(queryWithAnd);

        // Don't include simple quotes around the key or value
        Query queryA = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));
        Query queryB = new Query("pom","dependency", Map.of("artifactId", "quarkus-core","version", "3.16.2"));

        Set<Query> queries = visitor.getAndQueries();
        var queryList = queries.stream().toList();
        Assert.assertTrue(queryList.size() == 2);
        Assertions.assertEquals(queryList.get(0),queryA);
        Assertions.assertEquals(queryList.get(1),queryB);
    }

    @Test
    public void simpleQueryWithoutKeyValuePairs() {
        String simpleQuery = "java.annotation is '@SpringBootApplication'";
        QueryVisitor visitor = parseQuery(simpleQuery);

        // Should automatically use "name" as default key for annotation
        Query expectedQuery = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertTrue(queries.contains(expectedQuery));
    }

    @Test
    public void dependencyQueryWithoutKeyValuePairs() {
        String dependencyQuery = "pom.dependency is 'quarkus-core'";
        QueryVisitor visitor = parseQuery(dependencyQuery);

        // Should automatically use "artifactId" as default key for dependency
        Query expectedQuery = new Query("pom","dependency", Map.of("artifactId", "quarkus-core"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertTrue(queries.contains(expectedQuery));
    }

    @Test
    public void testDoubleQuotedAnnotationSyntax() {
        String annotationQuery = "java.annotation is \"@SpringBootApplication\"";
        QueryVisitor visitor = parseQuery(annotationQuery);

        // Should automatically use "name" as default key for annotation
        Query expectedQuery = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertTrue(queries.contains(expectedQuery));
    }

    @Test
    public void testMixedQuoteSyntax() {
        String queryWithMixedQuotes = "java.annotation is '@SpringBootApplication' AND pom.dependency is \"quarkus-core\"";
        QueryVisitor visitor = parseQuery(queryWithMixedQuotes);

        Query queryA = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));
        Query queryB = new Query("pom","dependency", Map.of("artifactId", "quarkus-core"));

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

        Query queryA = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));
        Query queryB = new Query("pom","dependency", Map.of("artifactId", "quarkus-core","version", "3.16.2"));

        Set<Query> queries = visitor.getAndQueries();
        var queryList = queries.stream().toList();
        Assert.assertTrue(queryList.size() == 2);
        Assertions.assertTrue(queries.contains(queryA));
        Assertions.assertTrue(queries.contains(queryB));
    }
}
