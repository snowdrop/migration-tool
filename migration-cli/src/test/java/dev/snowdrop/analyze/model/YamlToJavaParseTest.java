package dev.snowdrop.analyze.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlToJavaParseTest {

    static ObjectMapper mapper;

    @BeforeAll
    static void init() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    public void should_parse_yaml_to_java_object() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rule1.yaml");
        assertNotNull(inputStream, "rule1.yaml resource not found");

        List<Rule> rules = mapper.readValue(inputStream, new TypeReference<List<Rule>>() {
        });

        assertFalse(rules.isEmpty(), "No rules parsed from YAML");
        Rule rule = rules.get(0);

        assertEquals("springboot-annotations-to-quarkus-00000", rule.ruleID());
        assertEquals("mandatory", rule.category());
        assertEquals("Replace the Spring Boot Application Annotation with QuarkusMain and add Quarkus.run",
                rule.description());
        assertEquals(1, rule.effort());

        // Verify instructions
        assertNotNull(rule.instructions());
        assertNotNull(rule.instructions().ai());
        assertEquals(1, rule.instructions().ai().length);
        assertEquals(
                "Remove the org.springframework.boot.autoconfigure.SpringBootApplication annotation from the main Spring Boot Application class",
                rule.instructions().ai()[0].promptMessage());

        assertNotNull(rule.instructions().manual());
        assertEquals(1, rule.instructions().manual().length);
        assertEquals(
                "Remove the org.springframework.boot.autoconfigure.SpringBootApplication annotation from the main Spring Boot Application class",
                rule.instructions().manual()[0].todo());

        assertNotNull(rule.instructions().openrewrite());

        Rule.Openrewrite[] openrewrites = rule.instructions().openrewrite();
        assertEquals(1, openrewrites.length);
        assertEquals("Migrate Spring Boot to Quarkus", rule.instructions().openrewrite()[0].name());

        Rule.Precondition precondition = openrewrites[0].preconditions()[0];
        assertEquals("org.openrewrite.java.dependencies.search.ModuleHasDependency", precondition.name());
        assertEquals("org.springframework.boot", precondition.groupIdPattern());
        assertEquals("spring-boot", precondition.artifactIdPattern());

        List<Object> recipes = openrewrites[0].recipeList();
        Map<String, Map<String, String>> recipe = (Map) recipes.get(0);
        var recipeDetail = recipe.get("dev.snowdrop.openrewrite.java.search.FindAnnotations");
        assertEquals("org.springframework.stereotype.Controller", recipeDetail.get("pattern"));
        assertEquals("rule-001-001", recipeDetail.get("matchId"));
    }
}
