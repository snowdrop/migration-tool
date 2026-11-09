package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calls quarkit_validate with the ui validator through the MCP server. The projects are created under
 * target/, inside the default allowed directory (".").
 */
@QuarkusTest
public class ValidateUiMcpToolTest {

    private static final Path ROOT = Path.of("target", "validate-ui-mcp");

    private Path target;
    private Path source;

    @BeforeEach
    void createProjects() throws IOException {
        if (Files.exists(ROOT)) {
            try (Stream<Path> paths = Files.walk(ROOT)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        target = ROOT.resolve("quarkus");
        source = ROOT.resolve("spring");
        write(target.resolve("pom.xml"), "<project><dependencies><dependency><groupId>io.quarkus</groupId>"
                + "<artifactId>quarkus-rest-qute</artifactId></dependency></dependencies></project>");
        write(target.resolve("src/main/resources/templates/index.html"), "<p th:text=\"${name}\">x</p>");
        write(source.resolve("src/main/resources/templates/index.html"), "<p th:text=\"${name}\">x</p>");
    }

    @Test
    public void uiValidatorReportsRules() {
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "ui",
                "skipCompile", "true", "uiMigrationType", "thymeleaf-qute"),
                text -> {
                    assertTrue(text.startsWith("## ui"), text);
                    assertTrue(text.contains("[FAIL] no_thymeleaf_attributes"), text);
                    assertTrue(text.contains("[PASS] quarkus_rest_qute_dependency"), text);
                    assertTrue(text.contains("[PASS] source_target_parity"), text);
                    assertTrue(text.contains("[WARN] quarkus_build"), text);
                });
    }

    @Test
    public void mixedTypesQualifyRuleNames() {
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "ui",
                "skipCompile", "true", "uiMigrationType", "thymeleaf-qute,jsp-qute"),
                text -> {
                    assertTrue(text.contains("no_thymeleaf_attributes (thymeleaf-qute)"), text);
                    assertTrue(text.contains("no_jsp_files (jsp-qute)"), text);
                });
    }

    @Test
    public void allWithoutUiTypeReportsSkippedUi() {
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "all",
                "skipCompile", "true"),
                text -> assertTrue(
                        text.contains("[WARN] ui — not run: set uiMigrationType to validate the view layer"), text));
    }

    @Test
    public void errorsDocumentedInTheSkill() {
        call(Map.of("target", target.toString(), "validator", "persistence", "skipCompile", "true"),
                text -> assertTrue(text.startsWith("Error: source project path is required for the 'persistence'"),
                        text));
        call(Map.of("target", target.toString(), "validator", "ui", "skipCompile", "true",
                "uiMigrationType", "thymeleaf-qute"),
                text -> assertTrue(text.startsWith("Error: source project path is required for the 'ui'"), text));
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "ui",
                "skipCompile", "true"),
                text -> assertTrue(text.startsWith("Error: UI migration type cannot be empty"), text));
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "ui",
                "skipCompile", "true", "uiMigrationType", "angular"),
                text -> assertTrue(text.startsWith("Error: Invalid UI migration type: 'angular'"), text));
        call(Map.of("target", target.toString(), "source", source.toString(), "validator", "ui",
                "skipCompile", "true", "uiMigrationType", "jsf-qute,jsf-myfaces"),
                text -> assertTrue(text.contains("cannot be combined"), text));
    }

    private static void call(Map<String, Object> args, java.util.function.Consumer<String> assertion) {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_validate", args, response -> assertion.accept(response.content().get(0).asText().text()))
                .thenAssertResults();
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
