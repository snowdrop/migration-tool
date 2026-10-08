package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.persistence.EntityModel;
import dev.snowdrop.mtool.scanner.treesitter.TreeSitterJavaAnalyzer;
import dev.snowdrop.mtool.validate.persistence.PersistenceValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsEntityFromJavaSource() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);

        TreeSitterJavaAnalyzer analyzer = new TreeSitterJavaAnalyzer();
        AnalysisResult result = analyzer.analyze(springProject);

        assertThat(result.getSymbolTable()).isNotEmpty();
        assertThat(result.getSymbolTable().values().stream()
                .flatMap(cu -> cu.getTypeDeclarations().values().stream())
                .filter(t -> t.getAnnotations().stream().anyMatch(a -> a.contains("@Entity")))
                .count()).isEqualTo(1);
    }

    @Test
    void extractsTableName() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);

        TreeSitterJavaAnalyzer analyzer = new TreeSitterJavaAnalyzer();
        AnalysisResult result = analyzer.analyze(springProject);

        var types = result.getSymbolTable().values().iterator().next().getTypeDeclarations();
        var type = types.values().iterator().next();
        assertThat(type.getAnnotations()).anyMatch(a -> a.contains("@Table") && a.contains("todos"));
    }

    @Test
    void extractsFieldsWithAnnotations() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);

        TreeSitterJavaAnalyzer analyzer = new TreeSitterJavaAnalyzer();
        AnalysisResult result = analyzer.analyze(springProject);

        var types = result.getSymbolTable().values().iterator().next().getTypeDeclarations();
        var type = types.values().iterator().next();

        assertThat(type.getFieldDeclarations()).hasSizeGreaterThanOrEqualTo(3);

        var idField = type.getFieldDeclarations().stream()
                .filter(f -> f.getVariables().contains("id"))
                .findFirst().orElseThrow();
        assertThat(idField.getAnnotations()).anyMatch(a -> a.contains("@Id"));
        assertThat(idField.getAnnotations()).anyMatch(a -> a.contains("@GeneratedValue"));
        assertThat(idField.getType()).isEqualTo("Long");
    }

    @Test
    void extractsRelationshipAnnotations() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), ENTITY_WITH_RELATIONSHIP);

        TreeSitterJavaAnalyzer analyzer = new TreeSitterJavaAnalyzer();
        AnalysisResult result = analyzer.analyze(springProject);

        var types = result.getSymbolTable().values().iterator().next().getTypeDeclarations();
        var type = types.values().iterator().next();

        var itemsField = type.getFieldDeclarations().stream()
                .filter(f -> f.getVariables().contains("items"))
                .findFirst().orElseThrow();
        assertThat(itemsField.getAnnotations()).anyMatch(a -> a.contains("@OneToMany"));
        assertThat(itemsField.getType()).isEqualTo("List<Item>");
    }

    @Test
    void identicalProjectsPassValidation() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), SPRING_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isZero();
        assertThat(report.overallStatus()).isEqualTo("PASS");
    }

    @Test
    void detectsMissingEntity() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), DIFFERENT_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isGreaterThan(0);
        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing entities"));
    }

    @Test
    void detectsMissingField() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), ENTITY_MISSING_FIELD);

        PersistenceValidator validator = new PersistenceValidator(springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing field"));
    }

    @Test
    void detectsIdStrategyChange() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), ENTITY_DIFFERENT_ID_STRATEGY);

        PersistenceValidator validator = new PersistenceValidator(springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("ID strategy changed"));
    }

    @Test
    void entityCountMismatchFails() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = tempDir.resolve("quarkus");
        Files.createDirectories(quarkusProject.resolve("src/main/java"));

        PersistenceValidator validator = new PersistenceValidator(springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Entity count mismatch"));
    }

    private Path createProject(Path root, String entitySource) throws IOException {
        Path srcDir = root.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Todo.java"), entitySource);
        return root;
    }

    private static final String SPRING_ENTITY = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "todos")
            public class Todo {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;

                @Column(nullable = false)
                private String title;

                private String description;

                private boolean completed;
            }
            """;

    private static final String DIFFERENT_ENTITY = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "items")
            public class Item {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;

                private String name;
            }
            """;

    private static final String ENTITY_MISSING_FIELD = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "todos")
            public class Todo {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;

                private String title;
            }
            """;

    private static final String ENTITY_DIFFERENT_ID_STRATEGY = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "todos")
            public class Todo {
                @Id
                @GeneratedValue(strategy = GenerationType.AUTO)
                private Long id;

                @Column(nullable = false)
                private String title;

                private String description;

                private boolean completed;
            }
            """;

    private static final String ENTITY_WITH_RELATIONSHIP = """
            package com.example;

            import jakarta.persistence.*;
            import java.util.List;

            @Entity
            @Table(name = "orders")
            public class Order {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;

                @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
                private List<Item> items;
            }
            """;
}