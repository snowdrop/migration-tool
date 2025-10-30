package dev.snowdrop.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.mapper.QueryToRecipeMapper;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeDTOSerializer;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import dev.snowdrop.reconciler.MatchingUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class QueryToRecipeApp {

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) {
        QueryToRecipeApp app = new QueryToRecipeApp();
        String query;
        QueryVisitor visitor;

        System.out.println("=== Simple query with one clause ");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication')";
        visitor = QueryUtils.parseAndVisit(query);
        visitor.getSimpleQueries().forEach(q -> {
            app.logQueryResult(q);
            // Create for each Query the corresponding RecipeDTO
            RecipeDTO dto = QueryToRecipeMapper.map(q);
            dto = dto.withId(MatchingUtils.generateUID());
            System.out.println(dto);

            try {
                // convert the DTO to the YAML
                String yaml = yamlRecipeMapper().writeValueAsString(dto);
                System.out.println(yaml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("=========================");
        System.out.println("=== Query with clause AND clause ");
        System.out.println(
                "=== FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')";
        visitor = QueryUtils.parseAndVisit(query);
        visitor.getAndQueries().forEach(q -> {
            app.logQueryResult(q);
            // Create for each Query the corresponding RecipeDTO
            RecipeDTO dto = QueryToRecipeMapper.map(q);
            dto = dto.withId(MatchingUtils.generateUID());
            System.out.println(dto);

            try {
                // convert the DTO to the YAML
                String yaml = yamlRecipeMapper().writeValueAsString(dto);
                System.out.println(yaml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("=========================");
        System.out.println("=== Query with clause OR clause OR clause");
        System.out.println(
                "=== FIND java.annotation WHERE (name='@SpringBootApplication1') OR java.annotation WHERE (name='@SpringBootApplication2') OR java.annotation WHERE (name='@SpringBootApplication3')");
        query = """
                FIND java.annotation WHERE (name='@SpringBootApplication1') OR
                java.annotation WHERE (name='@SpringBootApplication2') OR
                java.annotation WHERE (name='@SpringBootApplication3')
                """;
        visitor = QueryUtils.parseAndVisit(query);
        visitor.getOrQueries().forEach(q -> {
            app.logQueryResult(q);
            // Create for each Query the corresponding RecipeDTO
            RecipeDTO dto = QueryToRecipeMapper.map(q);
            dto = dto.withId(MatchingUtils.generateUID());
            System.out.println(dto);

            try {
                // convert the DTO to the YAML
                String yaml = yamlRecipeMapper().writeValueAsString(dto);
                System.out.println(yaml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void logQueryResult(Query qr) {
        System.out.println("=== Query parsed ===");
        System.out.println(String.format("=== Type: %s", qr.fileType()));
        System.out.println(String.format("=== Symbol: %s", qr.symbol()));
        qr.keyValues().forEach((k, v) -> {
            System.out.println(String.format("Key: %s, value: %s.", k, v));
        });
    }

    public static ObjectMapper yamlRecipeMapper() {
        YAMLFactory factory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        ObjectMapper yamlMapper = new ObjectMapper(factory);

        SimpleModule module = new SimpleModule();
        module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
        yamlMapper.registerModule(module);

        return yamlMapper;
    }
}