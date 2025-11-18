package dev.snowdrop.mapper;

import dev.snowdrop.mapper.openrewrite.QueryToRecipeMapper;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class QueryMappedToRecipeTest {

	@Test
	public void shouldJavaAnnotationMapRecipeDTO() {
		Query q = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		// Map the query to its DTO
		RecipeDTO dto = QueryToRecipeMapper.map(q);
		Assertions.assertEquals("dev.snowdrop.openrewrite.java.search.FindAnnotations", dto.name());
		Assertions.assertEquals("pattern", dto.parameters().get(0).parameter());
		Assertions.assertEquals("@SpringBootApplication", dto.parameters().get(0).value());
	}

	@Test
	public void shouldPomDependencyMapRecipeDTO() {
		Query query = new Query("pom", "dependency", Map.of("gavs", "io.quarkus:quarkus-core:3.16.2", "groupId",
				"io.quarkus", "artifactId", "quarkus-core", "version", "3.16.2"));

		RecipeDTO dto = QueryToRecipeMapper.map(query);
		Assertions.assertEquals("dev.snowdrop.openrewrite.maven.search.FindDependency", dto.name());
		Assertions.assertEquals("io.quarkus:quarkus-core:3.16.2", dto.parameters().get(0).value());
		Assertions.assertEquals("io.quarkus", dto.parameters().get(1).value());
		Assertions.assertEquals("quarkus-core", dto.parameters().get(2).value());
		Assertions.assertEquals("3.16.2", dto.parameters().get(3).value());
	}

}
