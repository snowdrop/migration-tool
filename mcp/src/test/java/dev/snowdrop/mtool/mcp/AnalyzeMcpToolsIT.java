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
        Files.createDirectories(tempDir.resolve("src/main/java/demo"));
        Files.writeString(tempDir.resolve("src/main/java/demo/Hello.java"), "package demo; public class Hello {}");
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "key=value\n");

        try (McpAssured.McpStdioTestClient client = McpAssured.newStdioClient()
                .setStateless()
                .setCommand("java", "-Dmtool.allowed-dirs=" + tempDir.toString(), "-jar", "target/quarkus-app/quarkus-run.jar")
                .build()
                .connect()) {

            client.when()
                    .toolsCall("sourceAnalyze", Map.of("projectPath", tempDir.toString(),
                            "userQueries", "java class all;properties all"), r -> {
                                Assertions.assertFalse(r.isError());
                                String text = r.firstContent().asText().text();
                                Assertions.assertTrue(text.startsWith("Total: 2 match(es) across 2 query(ies)"));
                                Assertions.assertTrue(text.contains("src/main/java/demo/Hello.java"));
                                Assertions.assertTrue(text.contains("src/main/resources/application.properties"));
                            })
                    .thenAssertResults();
        }
    }
}
