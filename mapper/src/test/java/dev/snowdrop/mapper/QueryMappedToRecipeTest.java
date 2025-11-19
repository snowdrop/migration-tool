package dev.snowdrop.mapper;

import dev.snowdrop.mapper.java.annotation.JavaAnnotationMapper;
import dev.snowdrop.mapper.java.annotation.JdtlsJavaAnnotationMapper;
import dev.snowdrop.mapper.java.annotation.OpenRewriteJavaAnnotationMapper;
import dev.snowdrop.model.JavaAnnotationDTO;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class QueryMappedToRecipeTest {

	@Test
	public void shouldJavaAnnotationMapRecipeDTO() {
		Query q = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		// Map the query to its DTO (factory pattern returns Object, cast to RecipeDTO)
		JavaAnnotationMapper mapper = new JavaAnnotationMapper();
		Object result = mapper.map(q);
		Assertions.assertTrue(result instanceof RecipeDTO, "Expected RecipeDTO for OpenRewrite scanner");

		RecipeDTO dto = (RecipeDTO) result;
		Assertions.assertEquals("dev.snowdrop.openrewrite.java.search.FindAnnotations", dto.name());
		Assertions.assertEquals("pattern", dto.parameters().get(0).parameter());
		Assertions.assertEquals("@SpringBootApplication", dto.parameters().get(0).value());
	}

	@Test
	public void shouldThrowExceptionForUnsupportedQuery() {
		Query q = new Query("pom", "dependency", Map.of("gavs", "io.quarkus:quarkus-core:3.16.2", "groupId",
				"io.quarkus", "artifactId", "quarkus-core", "version", "3.16.2"));

		// JavaAnnotationMapper should only handle java.annotation queries
		// This should throw an exception for pom.dependency queries
		JavaAnnotationMapper mapper = new JavaAnnotationMapper();
		Assertions.assertThrows(IllegalArgumentException.class, () -> mapper.map(q),
				"Should throw IllegalArgumentException for unsupported query type");
	}

	@Test
	public void shouldOpenRewriteMapperReturnRecipeDTO() {
		Query q = new Query("java", "annotation", Map.of("name", "@Component"));

		// Test OpenRewrite scanner-specific mapper directly
		OpenRewriteJavaAnnotationMapper mapper = new OpenRewriteJavaAnnotationMapper();
		Object result = mapper.map(q);
		Assertions.assertTrue(result instanceof RecipeDTO, "OpenRewrite mapper should return RecipeDTO");

		RecipeDTO dto = (RecipeDTO) result;
		Assertions.assertEquals("dev.snowdrop.openrewrite.java.search.FindAnnotations", dto.name());
		Assertions.assertEquals("pattern", dto.parameters().get(0).parameter());
		Assertions.assertEquals("@Component", dto.parameters().get(0).value());
	}

	@Test
	public void shouldJdtlsMapperReturnJavaAnnotationDTO() {
		Query q = new Query("java", "annotation", Map.of("name", "@Service"));

		// Test JDTLS scanner-specific mapper directly
		JdtlsJavaAnnotationMapper mapper = new JdtlsJavaAnnotationMapper();
		Object result = mapper.map(q);
		Assertions.assertTrue(result instanceof JavaAnnotationDTO, "JDTLS mapper should return JavaAnnotationDTO");

		JavaAnnotationDTO dto = (JavaAnnotationDTO) result;
		Assertions.assertEquals("@Service", dto.annotationName());
		Assertions.assertTrue(dto.attributes().containsKey("name"));
		Assertions.assertEquals("@Service", dto.attributes().get("name"));
	}

	@Test
	public void shouldJdtlsMapperThrowExceptionForMissingName() {
		Query q = new Query("java", "annotation", Map.of("invalid", "value"));

		// JDTLS mapper requires 'name' parameter
		JdtlsJavaAnnotationMapper mapper = new JdtlsJavaAnnotationMapper();
		Assertions.assertThrows(IllegalArgumentException.class, () -> mapper.map(q),
				"Should throw IllegalArgumentException when 'name' parameter is missing");
	}

}
