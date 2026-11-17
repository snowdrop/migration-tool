package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpAssert;
import io.quarkiverse.mcp.server.ToolResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Launches the packaged MCP server and calls the quarkit_analyze tool using the transport the server was built with
 * (system property mcp.it.transport, set by the pom profiles all/stdio/http): stdio, or streamable HTTP.
 */
public class AnalyzeMcpToolsIT {

    private static final String RUNNER_JAR = "target/quarkus-app/quarkus-run.jar";
    private static final Duration HTTP_STARTUP_TIMEOUT = Duration.ofSeconds(60);

    @Test
    public void testToolsCall(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/demo"));
        Files.writeString(tempDir.resolve("src/main/java/demo/Hello.java"), "package demo; public class Hello {}");
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "key=value\n");

        String allowedDirs = "-Dquarkit.allowed-dirs=" + tempDir.toAbsolutePath();
        if ("http".equals(System.getProperty("mcp.it.transport", "stdio"))) {
            callOverHttp(tempDir, allowedDirs);
        } else {
            callOverStdio(tempDir, allowedDirs);
        }
    }

    private void callOverStdio(Path tempDir, String allowedDirs) {
        try (McpAssured.McpStdioTestClient client = McpAssured.newStdioClient()
                .setStateless()
                .setCommand("java", allowedDirs, "-jar", RUNNER_JAR)
                .build()
                .connect()) {
            analyze(client.when(), tempDir).thenAssertResults();
        }
    }

    private void callOverHttp(Path tempDir, String allowedDirs) throws Exception {
        int port = freePort();
        File log = tempDir.resolve("mcp-server.log").toFile();
        Process server = new ProcessBuilder("java", allowedDirs, "-Dquarkus.http.port=" + port, "-jar", RUNNER_JAR)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start();
        try {
            waitForPort(server, port, log);
            try (McpAssured.McpStreamableTestClient client = McpAssured.newStreamableClient()
                    .setBaseUri(URI.create("http://localhost:" + port))
                    .build()
                    .connect()) {
                analyze(client.when(), tempDir).thenAssertResults();
            }
        } finally {
            server.destroy();
            if (!server.waitFor(10, TimeUnit.SECONDS)) {
                server.destroyForcibly();
            }
        }
    }

    private <A extends McpAssert<A>> A analyze(A assertion, Path tempDir) {
        return assertion.toolsCall("quarkit_analyze", Map.of("project_path", tempDir.toString(),
                "query", "java class all;properties all"), AnalyzeMcpToolsIT::assertAnalyzeResponse);
    }

    private static void assertAnalyzeResponse(ToolResponse r) {
        Assertions.assertFalse(r.isError());
        String text = r.firstContent().asText().text();
        Assertions.assertTrue(text.startsWith("Total: 2 match(es) across 2 query(ies)"), text);
        Assertions.assertTrue(text.contains("src/main/java/demo/Hello.java"), text);
        Assertions.assertTrue(text.contains("src/main/resources/application.properties"), text);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForPort(Process server, int port, File log) throws Exception {
        Instant deadline = Instant.now().plus(HTTP_STARTUP_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (!server.isAlive()) {
                Assertions.fail("The MCP server exited with code " + server.exitValue() + ":\n"
                        + Files.readString(log.toPath()));
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port), 500);
                return;
            } catch (IOException e) {
                Thread.sleep(250);
            }
        }
        Assertions.fail("The MCP server did not listen on port " + port + " within " + HTTP_STARTUP_TIMEOUT + ":\n"
                + Files.readString(log.toPath()));
    }
}
