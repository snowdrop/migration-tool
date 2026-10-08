package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.test.McpAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class AnalyzeToolsTest {
    @Test
    public void testToolsCall() {
        try (McpAssured.McpStdioTestClient client = McpAssured.newStdioClient()
                .setStateless()
                .setCommand("java", "-jar", "target/mcp-1.0.7-SNAPSHOT-runner.jar")
                .build()
                .connect()) {

            client.when()
                    .toolsCall("sourceAnalyze", Map.of("projectPath", "/path/to/project"), r -> {
                        Assertions.assertFalse(r.isError());
                        Assertions.assertEquals("Analyzing the source: /path/to/project", r.firstContent().asText().text());
                    })
                    .thenAssertResults();
        }
    }
}
