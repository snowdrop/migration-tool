package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.test.McpAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AnalyzeMcpToolsIT {
    @Test
    public void testToolsCall(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.writeString(tempDir.resolve("src/main/java/Hello.java"), "public class Hello {}");
        Files.writeString(tempDir.resolve("application.properties"), "key=value");

        try (McpAssured.McpStdioTestClient client = McpAssured.newStdioClient()
                .setStateless()
                .setCommand("java", "-jar", "target/quarkus-app/quarkus-run.jar")
                .build()
                .connect()) {

            client.when()
                    .toolsCall("sourceAnalyze", Map.of("projectPath", tempDir.toString()), r -> {
                        Assertions.assertFalse(r.isError());
                        String text = r.firstContent().asText().text();
                        Assertions.assertTrue(text.startsWith("Found 2 file(s)"));
                        Assertions.assertTrue(text.contains("src/main/java/Hello.java"));
                        Assertions.assertTrue(text.contains("application.properties"));
                    })
                    .thenAssertResults();
        }
    }
}
