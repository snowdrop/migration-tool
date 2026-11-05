package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.*;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonArray;
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
                    assertEquals(4, page.size());
                    McpAssured.ToolInfo analyze = page.findByName("quarkit_analyze");
                    assertEquals("quarkit_analyze", analyze.name());

                    JsonObject sourceAnalyzeSchema = analyze.inputSchema();
                    JsonObject sourceAnalyzeProperties = sourceAnalyzeSchema.getJsonObject("properties");
                    assertNotNull(sourceAnalyzeProperties.getJsonObject("project_path"), sourceAnalyzeProperties.toString());
                    assertNotNull(sourceAnalyzeProperties.getJsonObject("query"), sourceAnalyzeProperties.toString());

                    McpAssured.ToolInfo validate = page.findByName("quarkit_validate");
                    assertEquals("quarkit_validate", validate.name());
                    assertEquals("quarkit_references_list", page.findByName("quarkit_references_list").name());
                    assertEquals("quarkit_reference_lookup", page.findByName("quarkit_reference_lookup").name());
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

    @Test
    public void testReferencesList() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_references_list", Map.of(), toolResponse -> {
                    JsonArray maps = new JsonArray(toolResponse.content().get(0).asText().text());
                    assertEquals(3, maps.size());
                    assertEquals("annotation", maps.getJsonObject(0).getString("map"));
                    assertEquals("config", maps.getJsonObject(1).getString("map"));
                    assertEquals("dependency", maps.getJsonObject(2).getString("map"));
                    assertTrue(maps.getJsonObject(0).getJsonArray("sections").contains("Dependency Injection"));
                    assertTrue(maps.getJsonObject(0).getInteger("records") > 0);
                })
                .thenAssertResults();
    }

    @Test
    public void testReferenceLookupHit() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_reference_lookup", Map.of("map", "annotation", "key", "@Primary"), toolResponse -> {
                    JsonArray rows = new JsonArray(toolResponse.content().get(0).asText().text());
                    assertEquals(1, rows.size());
                    JsonObject row = rows.getJsonObject(0);
                    assertEquals("@Primary", row.getString("spring"));
                    assertEquals("NOT_SUPPORTED", row.getString("compat"));
                    assertNotNull(row.getString("notes"));
                })
                .thenAssertResults();
    }

    @Test
    public void testReferenceLookupCompatMode() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_reference_lookup",
                        Map.of("map", "dependency", "key", "spring-boot-starter-web", "mode", "compat"),
                        toolResponse -> {
                            JsonArray rows = new JsonArray(toolResponse.content().get(0).asText().text());
                            assertFalse(rows.isEmpty());
                            JsonObject row = rows.getJsonObject(0);
                            assertEquals("SUPPORTED", row.getString("compat"));
                            assertNull(row.getString("notes"));
                            assertNull(row.getString("section"));
                        })
                .thenAssertResults();
    }

    @Test
    public void testReferenceLookupMiss() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_reference_lookup", Map.of("map", "annotation", "key", "@DoesNotExist"),
                        toolResponse -> {
                            assertEquals(0, new JsonArray(toolResponse.content().get(0).asText().text()).size());
                        })
                .thenAssertResults();
    }

    @Test
    public void testReferenceLookupUnknownMap() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_reference_lookup", Map.of("map", "unknown", "key", "x"), toolResponse -> {
                    assertTrue(new JsonObject(toolResponse.content().get(0).asText().text()).containsKey("error"));
                })
                .thenAssertResults();
    }

    @Test
    public void testReferenceResources() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .resourcesList(page -> assertNotNull(page.findByUri("mtool://references/annotation-map")))
                .resourcesRead("mtool://references/annotation-map", response -> {
                    assertTrue(new JsonArray(response.contents().get(0).asText().text()).size() > 0);
                })
                .resourcesTemplatesList(page -> assertNotNull(page.findByUriTemplate("mtool://references/{map}/{key}")))
                .thenAssertResults();
    }
}
