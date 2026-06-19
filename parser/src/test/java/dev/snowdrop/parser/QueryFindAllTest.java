package dev.snowdrop.parser;

import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.parser.QueryVisitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class QueryFindAllTest extends AbstractQueryParser {

    @Test
    public void queryFindAllJavaClasses() {
        String queryWithAll = "find all java.class";
        QueryVisitor visitor = parseQuery(queryWithAll);

        Query queryA = new Query("java", "class", "find all", Map.of());

        Set<Query> queries = visitor.getSimpleQueries();
        var queryList = queries.stream().toList();
        Assertions.assertEquals(1, queryList.size());
        Assertions.assertEquals(queryA, queryList.get(0));
    }
}
