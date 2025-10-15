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
        String simpleQuery = "FIND java.annotation WHERE (name='@SpringBootApplication')";
        QueryVisitor visitor = parseQuery(simpleQuery);

        // Don't include simple quotes around the key or value
        Query query = new Query("java","annotation", Map.of("name", "@SpringBootApplication"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertEquals(queries.stream().findFirst().get(), query);
    }

    @Test
    public void queryWithAnd() {
        String queryWithAnd = "FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')";
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
}
