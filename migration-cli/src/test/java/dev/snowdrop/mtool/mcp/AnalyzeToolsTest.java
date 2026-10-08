package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.test.McpAssured;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AnalyzeToolsTest {
    @Test
    public void testToolsCall() {
        try (McpAssured.McpStdioTestClient client = McpAssured.newStdioClient()
                .setStateless()
                .setCommand("java", "-jar", "target/migration-cli-1.0.7-SNAPSHOT-runner.jar")
                .build()
                .connect()) {

            client.when()
                    .toolsCall("sourceAnalyze", Map.of("value", "/path/to/project"), r -> {
                        assertFalse(r.isError());
                        assertEquals("Analyzing the source: /path/to/project", r.firstContent().asText().text());
                    })
                    .thenAssertResults();
        }
    }
}
