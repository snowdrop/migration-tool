package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.*;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import io.quarkiverse.mcp.server.Content.Type;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class McpToolsTest {

    @Test
    public void verifyTools() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsList(page -> {
                    assertEquals(2, page.size());
                    McpAssured.ToolInfo analyze = page.findByName("quarkit_analyze");
                    assertEquals("quarkit_analyze", analyze.name());

                    JsonObject sourceAnalyzeSchema = analyze.inputSchema();
                    JsonObject sourceAnalyzeProperties = sourceAnalyzeSchema.getJsonObject("properties");
                    assertNotNull(sourceAnalyzeProperties.getJsonObject("project_path"), sourceAnalyzeProperties.toString());
                    assertNotNull(sourceAnalyzeProperties.getJsonObject("query"), sourceAnalyzeProperties.toString());

                    McpAssured.ToolInfo validate = page.findByName("quarkit_validate");
                    assertEquals("quarkit_validate", validate.name());
                })
                .thenAssertResults();
    }

    @Test
    public void testSourceAnalyzeResponse(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java/demo"));
        Files.writeString(tempDir.resolve("src/main/java/demo/Hello.java"), "package demo; public class Hello {}");
        String result = """
                Total: 1 match(es) across 1 query(ies)

                ## Query: java class all
                Found 1 match(es):
                Path: src/main/java/demo/Hello.java, start: (1, 28), end: (1-33), text: Hello""";

        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_analyze", Map.of("project_path", tempDir.toString(), "query", "java class all"),
                        toolResponse -> {
                            assertEquals(1, toolResponse.content().size());
                            for (Content c : toolResponse.content()) {
                                assertEquals(result, c.asText().text());
                            }
                        })
                .thenAssertResults();
    }
}
