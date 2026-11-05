package dev.snowdrop.mtool.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonArray;
import org.junit.jupiter.api.Test;

/**
 * Verifies that mtool.references.path takes precedence over the classpath references.
 */
@QuarkusTest
@TestProfile(ReferencesPathOverrideTest.ReferencesPathProfile.class)
public class ReferencesPathOverrideTest {

    @Test
    public void testOverrideWinsAndMissingFileFallsBackToClasspath() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();
        client.when()
                .toolsCall("quarkit_reference_lookup", Map.of("map", "annotation", "key", "@Primary"),
                        toolResponse -> {
                            JsonArray rows = new JsonArray(toolResponse.content().get(0).asText().text());
                            assertEquals(1, rows.size());
                            assertEquals("SUPPORTED", rows.getJsonObject(0).getString("compat"));
                            assertEquals("override", rows.getJsonObject(0).getString("notes"));
                        })
                .toolsCall("quarkit_reference_lookup", Map.of("map", "config", "key", "server.port"),
                        toolResponse -> {
                            // config-map.json is absent from the override path: classpath fallback
                            assertTrue(new JsonArray(toolResponse.content().get(0).asText().text()).size() > 0);
                        })
                .thenAssertResults();
    }

    public static class ReferencesPathProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            try {
                Path dir = Files.createTempDirectory("mtool-references");
                Files.writeString(dir.resolve("annotation-map.json"), """
                        [{"section":"Dependency Injection","spring":"@Primary","quarkus":"@Primary",
                          "compat":"SUPPORTED","notes":"override"}]""");
                return Map.of("mtool.references.path", dir.toString());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
