package dev.snowdrop.mtool.scanner.treesitter;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;
import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Result;
import dev.snowdrop.mtool.model.parser.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeSitterAnnotationArgsTest {

    private TreeSitterQueryScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new TreeSitterQueryScanner();
    }

    @Test
    void scanAnnotation_capturesTableWithArguments() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("Order.java"), """
                package com.example;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = TableNames.ORDERS)
                public class Order {

                    @Id
                    private Long id;
                    private String description;
                }
                """);

        Config config = new Config(tempDir.toString(), null, null, null, false, null, "treesitter", null);
        Query query = new Query("java", "annotation", "all", Collections.emptyMap());

        List<Result> matches = scanner.scansCodeFor(config, query);

        assertEquals(3, matches.size(), "Expected 3 annotations: @Entity, @Table(...), @Id");

        assertTrue(matches.stream().allMatch(r -> r.result() instanceof AnnotationInfo));

        AnnotationInfo entity = (AnnotationInfo) matches.get(0).result();
        assertEquals("Entity", entity.name());
        assertTrue(entity.arguments().isEmpty());

        AnnotationInfo table = (AnnotationInfo) matches.get(1).result();
        assertEquals("Table", table.name());
        assertTrue(table.hasArgument("name", "TableNames.ORDERS"));
        assertEquals("TableNames.ORDERS", table.getArgument("name"));

        AnnotationInfo id = (AnnotationInfo) matches.get(2).result();
        assertEquals("Id", id.name());
        assertTrue(id.arguments().isEmpty());
    }
}
