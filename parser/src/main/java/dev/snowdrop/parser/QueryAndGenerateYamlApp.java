package dev.snowdrop.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.mapper.QueryToRecipeMapper;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeDTOSerializer;
import dev.snowdrop.parser.antlr.QueryLexer;
import dev.snowdrop.parser.antlr.QueryParser;
import dev.snowdrop.reconciler.KeyGenerator;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.concurrent.atomic.AtomicInteger;

public class QueryAndGenerateYamlApp {

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) {
        QueryAndGenerateYamlApp app = new QueryAndGenerateYamlApp();
        String query;
        QueryVisitor visitor;


        System.out.println("=== Simple query with one clause ");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication')";
        visitor = app.parseQuery(query);
        visitor.getSimpleQueries().forEach(q -> {
                app.logQueryResult(q);
                // Create for each Query the corresponding RecipeDTO
                RecipeDTO dto = QueryToRecipeMapper.map(q);
                System.out.println(dto);
            }
        );

        System.out.println("=========================");
        System.out.println("=== Query with clause AND clause ");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')";
        visitor = app.parseQuery(query);
        visitor.getAndQueries().forEach(q -> {
                app.logQueryResult(q);
                // Create for each Query the corresponding RecipeDTO
                RecipeDTO dto = QueryToRecipeMapper.map(q);
                System.out.println(dto);

                // Generate the YAML from the RecipeDTO
                YAMLFactory factory = new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
                ObjectMapper yamlMapper = new ObjectMapper(factory);
                //.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                //.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
                SimpleModule module = new SimpleModule();
                module.addSerializer(RecipeDTO.class, new RecipeDTOSerializer());
                yamlMapper.registerModule(module);

                try {
                    String yaml = yamlMapper.writeValueAsString(dto);
                    System.out.println(yaml);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );

        System.out.println("=========================");
        System.out.println("=== Query with clause OR clause OR clause");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication1') OR java.annotation WHERE (name='@SpringBootApplication2') OR java.annotation WHERE (name='@SpringBootApplication3')");
        query = """
            FIND java.annotation WHERE (name='@SpringBootApplication1') OR
            java.annotation WHERE (name='@SpringBootApplication2') OR
            java.annotation WHERE (name='@SpringBootApplication3')
            """;
        visitor = app.parseQuery(query);
        visitor.getOrQueries().forEach(q -> {
                app.logQueryResult(q);
                // Create for each Query the corresponding RecipeDTO
                RecipeDTO dto = QueryToRecipeMapper.map(q);
                System.out.println(dto);
                System.out.printf("Generated key: %s%n", KeyGenerator.generate(dto.name()));
                System.out.println();
            }
        );
    }

    private void logQueryResult(Query qr) {
        System.out.println("=== Query parsed ===");
        System.out.println(String.format("=== Type: %s", qr.fileType()));
        System.out.println(String.format("=== Symbol: %s", qr.symbol()));
        qr.keyValues().forEach((k, v) -> {
            System.out.println(String.format("Key: %s, value: %s.", k, v));
        });
    }

    private QueryVisitor parseQuery(String query) {
        try {
            ANTLRInputStream input = new ANTLRInputStream(query);
            QueryLexer lexer = new QueryLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            QueryParser parser = new QueryParser(tokens);
            ParseTree tree = parser.searchQuery();

            // Create and use the visitor
            QueryVisitor visitor = new QueryVisitor();
            visitor.visit(tree);

            return visitor;
        } catch (Exception e) {
            System.err.println("Error parsing query: " + query);
            e.printStackTrace();
            return new QueryVisitor();
        }
    }
}