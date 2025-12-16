package dev.snowdrop.parser;

import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.parser.QueryVisitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;
import java.util.Set;

public class QueryWithOrOperatorTest extends AbstractQueryParser {

	@Test
	public void queryWithOr() {
		String queryWithOr = "java.annotation is '@SpringBootApplication' OR java.annotation is '@Deprecated' ";
		QueryVisitor visitor = parseQuery(queryWithOr);

		// Don't include simple quotes around the key or value
		Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
		Query queryB = new Query("java", "annotation", Map.of("name", "@Deprecated"));

		Set<Query> queries = visitor.getOrQueries();
		var queryList = queries.stream().toList();
		Assert.assertTrue(queryList.size() == 2);
		Assertions.assertEquals(queryList.get(0), queryA);
		Assertions.assertEquals(queryList.get(1), queryB);
	}
}
