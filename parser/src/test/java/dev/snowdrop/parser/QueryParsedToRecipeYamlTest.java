package dev.snowdrop.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.mapper.QueryToRecipeMapper;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeDTOSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;
import java.util.Set;

public class QueryParsedToRecipeYamlTest extends AbstractQueryParser {
    @Test
    public void queryToYamlRecipe() {
        String simpleQuery = "FIND java.annotation WHERE (name='@SpringBootApplication')";
        QueryVisitor visitor = parseQuery(simpleQuery);

        // Don't include simple quotes around the key or value
        Query query = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));

        Set<Query> queries = visitor.getSimpleQueries();
        Assert.assertTrue(queries.size() == 1);
        Assertions.assertEquals(queries.stream().findFirst().get(), query);

        queries.stream().forEach(q -> {
            // Create for each Query the corresponding RecipeDTO
            RecipeDTO dto = QueryToRecipeMapper.map(q);

            // Generate the YAML from the RecipeDTO
            YAMLFactory factory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
            ObjectMapper yamlMapper = new ObjectMapper(factory);

            SimpleModule module = new SimpleModule();
            module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
            yamlMapper.registerModule(module);

            String expectedYaml = """
                dev.snowdrop.openrewrite.java.search.FindAnnotations:
                  pattern: "@SpringBootApplication"
                  matchId: "rule-001-003"
                  matchOnMetaAnnotations: "false"
                """;
            String generatedYaml = null;
            try {
                generatedYaml = yamlMapper.writeValueAsString(dto);
                Assertions.assertEquals(expectedYaml, generatedYaml);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void queryWithAnd() {
        String queryWithAnd = "FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')";
        QueryVisitor visitor = parseQuery(queryWithAnd);

        // Don't include simple quotes around the key or value
        Query queryA = new Query("java", "annotation", Map.of("name", "@SpringBootApplication"));
        Query queryB = new Query("pom", "dependency", Map.of("artifactId", "quarkus-core", "version", "3.16.2"));

        Set<Query> queries = visitor.getAndQueries();
        var queryList = queries.stream().toList();
        Assert.assertTrue(queryList.size() == 2);
        Assertions.assertEquals(queryList.get(0), queryA);
        Assertions.assertEquals(queryList.get(1), queryB);

        YAMLFactory factory = new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        ObjectMapper yamlMapper = new ObjectMapper(factory);

        SimpleModule module = new SimpleModule();
        module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
        yamlMapper.registerModule(module);

        String generatedYaml = null;
        try {
            RecipeDTO dto = QueryToRecipeMapper.map(queryList.getFirst());
            generatedYaml = yamlMapper.writeValueAsString(dto);

            // Check Query 1 => RecipeDTO => Yaml
            String expectedYaml = """
            dev.snowdrop.openrewrite.java.search.FindAnnotations:
              pattern: "@SpringBootApplication"
              matchId: "rule-001-001"
              matchOnMetaAnnotations: "false"
            """;
            Assertions.assertEquals(expectedYaml, generatedYaml);

            // Check Query 2 => RecipeDTO => Yaml
            expectedYaml = """
            org.openrewrite.maven.search.FindDependency:
              artifactId: "quarkus-core"
              version: "3.16.2"
              matchId: "rule-001-002"
            """;
            dto = QueryToRecipeMapper.map(queryList.get(1));
            generatedYaml = yamlMapper.writeValueAsString(dto);
            Assertions.assertEquals(expectedYaml, generatedYaml);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

}
