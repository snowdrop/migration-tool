package dev.snowdrop.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.mapper.openrewrite.QueryToRecipeMapper;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeDTOSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

@Disabled
public class QueryMappedToRecipeYamlTest {

	private static ObjectMapper yamlMapper;

	@BeforeAll
	public static void setup() {
		YAMLFactory factory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		yamlMapper = new ObjectMapper(factory);

		SimpleModule module = new SimpleModule();
		module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
		yamlMapper.registerModule(module);
	}

	@Test
	public void shouldJavaAnnotationMapRecipeDTO() {
		Query q = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

		// Map the query to its DTO
		RecipeDTO dto = QueryToRecipeMapper.map(q);

		// Get the UUID created as dto parameter
		String matchId = dto.parameters().stream().filter(p -> p.parameter().equals("matchId")).map(p -> p.value())
				.findAny().orElse(null);

		String expectedYaml = String.format("""
				dev.snowdrop.openrewrite.java.search.FindAnnotations:
				  pattern: "@SpringBootApplication"
				  matchId: "%s"
				  matchOnMetaAnnotations: "false"
				""", matchId);
		String generatedYaml = null;
		try {
			generatedYaml = yamlMapper.writeValueAsString(dto);
			Assertions.assertEquals(expectedYaml, generatedYaml);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void shouldPomDependencyMapRecipeDTO() {
		Query query = new Query("pom", "dependency", Map.of("gavs", "io.quarkus:quarkus-core:3.16.2", "groupId",
				"io.quarkus", "artifactId", "quarkus-core", "version", "3.16.2"));

		String generatedYaml = null;
		try {
			RecipeDTO dto = QueryToRecipeMapper.map(query);
			generatedYaml = yamlMapper.writeValueAsString(dto);

			// Get the UUID created as dto parameter
			String matchId = dto.parameters().stream().filter(p -> p.parameter().equals("matchId")).map(p -> p.value())
					.findAny().orElse(null);

			String expectedYaml = String.format("""
					dev.snowdrop.openrewrite.maven.search.FindDependency:
					  version: "3.16.2"
					  gavs: "io.quarkus:quarkus-core:3.16.2"
					  artifactId: "quarkus-core"
					  groupId: "io.quarkus"
					  matchId: %s
					""", matchId);
			Assertions.assertEquals(expectedYaml, generatedYaml);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
