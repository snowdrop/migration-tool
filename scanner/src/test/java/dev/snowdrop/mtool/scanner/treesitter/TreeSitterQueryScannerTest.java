package dev.snowdrop.mtool.scanner.treesitter;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TreeSitterQueryScannerTest {

    private TreeSitterQueryScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new TreeSitterQueryScanner();
    }

    @Test
    void supportsExpectedQueryTypes() {
        assertEquals("treesitter", scanner.getScannerType());
        assertTrue(scanner.supports(new Query("java", "annotation", "all", Collections.emptyMap())));
        assertTrue(scanner.supports(new Query("java", "class", "all", Collections.emptyMap())));
        assertTrue(scanner.supports(new Query("pom", "dependency", "all", Collections.emptyMap())));
        assertTrue(scanner.supports(new Query("properties", "", "all", Collections.emptyMap())));
    }

    @Test
    @Disabled
    void scanJavaAnnotation_findsByFqn() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Endpoint.java"), """
                package com.example;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                @Path("/api")
                public class Endpoint {

                    @GET
                    public String hello() {
                        return "Hello";
                    }
                }
                """);

        Config config = new Config(tempDir.toString(), null, null, null, null, null, null, false, null, "treesitter", null);
        Query query = new Query("java", "annotation", "", Map.of("name", "jakarta.ws.rs.Path"));

        List<Match> matches = scanner.scansCodeFor(config, query);

        assertEquals(1, matches.size());
        assertEquals("treesitter", matches.get(0).scannerType());
        assertTrue(matches.get(0).result().toString().contains("@Path"));
    }

    @Test
    @Disabled
    void scanJavaClass_findsByName() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("TaskController.java"), """
                package com.example;

                public class TaskController {
                }
                """);

        Config config = new Config(tempDir.toString(), null, null, null, null, null, null, false, null, "treesitter", null);
        Query query = new Query("java", "class", "", Map.of("name", "TaskController"));

        List<Match> matches = scanner.scansCodeFor(config, query);

        assertEquals(1, matches.size());
        assertTrue(matches.get(0).result().toString().contains("TaskController"));
    }

    @Test
    void scanPomDependency_findsByGav() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>com.mysql</groupId>
                            <artifactId>mysql-connector-j</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        Config config = new Config(tempDir.toString(), null, null, null, null, null, null, false, null, "treesitter", null);
        Query query = new Query("pom", "dependency", "",
                Map.of("gavs", "org.springframework.boot:spring-boot-starter-web"));

        List<Match> matches = scanner.scansCodeFor(config, query);

        assertEquals(1, matches.size());
        assertTrue(matches.get(0).result().toString().contains("org.springframework.boot:spring-boot-starter-web"));
    }
}
