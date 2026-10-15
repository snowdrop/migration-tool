package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.scanner.analyzer.JavaAnalyzer;
import dev.snowdrop.mtool.scanner.analyzer.TreeSitterAnalyzer;
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

        JavaAnalyzer analyzer = new TreeSitterAnalyzer();
        List<JavaTypeDeclaration> types = analyzer.extractTypes(springProject);

        assertThat(types).isNotEmpty();
        assertThat(types.stream()
                .filter(t -> t.annotations().stream().anyMatch(a -> "Entity".equals(a.name())))
                .count()).isEqualTo(1);
    }

    @Test
    void extractsTableName() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);

        JavaAnalyzer analyzer = new TreeSitterAnalyzer();
        List<JavaTypeDeclaration> types = analyzer.extractTypes(springProject);

        JavaTypeDeclaration type = types.get(0);
        assertThat(type.annotations()).anyMatch(a -> "Table".equals(a.name())
                && a.getArgument("name") != null && a.getArgument("name").contains("todos"));
    }

    @Test
    void extractsFieldsWithAnnotations() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);

        JavaAnalyzer analyzer = new TreeSitterAnalyzer();
        List<JavaTypeDeclaration> types = analyzer.extractTypes(springProject);

        JavaTypeDeclaration type = types.get(0);
        assertThat(type.fields()).hasSizeGreaterThanOrEqualTo(3);

        var idField = type.fields().stream()
                .filter(f -> "id".equals(f.name()))
                .findFirst().orElseThrow();
        assertThat(idField.annotations()).anyMatch(a -> "Id".equals(a.name()));
        assertThat(idField.annotations()).anyMatch(a -> "GeneratedValue".equals(a.name()));
        assertThat(idField.typeName()).isEqualTo("Long");
    }

    @Test
    void extractsRelationshipAnnotations() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), ENTITY_WITH_RELATIONSHIP);

        JavaAnalyzer analyzer = new TreeSitterAnalyzer();
        List<JavaTypeDeclaration> types = analyzer.extractTypes(springProject);

        JavaTypeDeclaration type = types.get(0);
        var itemsField = type.fields().stream()
                .filter(f -> "items".equals(f.name()))
                .findFirst().orElseThrow();
        assertThat(itemsField.annotations()).anyMatch(a -> "OneToMany".equals(a.name()));
        assertThat(itemsField.typeName()).isEqualTo("List<Item>");
    }

    @Test
    void identicalProjectsPassValidation() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), SPRING_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isZero();
        assertThat(report.overallStatus()).isEqualTo("PASS");
    }

    @Test
    void detectsMissingEntity() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), DIFFERENT_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isGreaterThan(0);
        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing entities"));
    }

    @Test
    void detectsMissingField() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), ENTITY_MISSING_FIELD);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing field"));
    }

    @Test
    void detectsIdStrategyChange() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), ENTITY_DIFFERENT_ID_STRATEGY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("ID strategy changed"));
    }

    @Test
    void constantReferenceInTableNameEmitsWarning() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), ENTITY_WITH_CONSTANT_TABLE_NAME);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), SPRING_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults())
                .anyMatch(r -> r.status() == ValidationResult.Status.WARNING
                        && r.evidence().contains("Cannot resolve constant reference"));
        assertThat(report.getResults())
                .noneMatch(r -> r.status() == ValidationResult.Status.FAILED
                        && r.evidence().contains("Table mismatch"));
    }

    private static final String ENTITY_WITH_CONSTANT_TABLE_NAME = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = TableConstants.TODOS)
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

    @Test
    void entityCountMismatchFails() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = tempDir.resolve("quarkus");
        Files.createDirectories(quarkusProject.resolve("src/main/java"));

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
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

    private Path createProject(Path root, String... sources) throws IOException {
        Path srcDir = root.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        for (String source : sources) {
            String className = extractClassName(source);
            Files.writeString(srcDir.resolve(className + ".java"), source);
        }
        return root;
    }

    private static String extractClassName(String source) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("class\\s+(\\w+)").matcher(source);
        return m.find() ? m.group(1) : "Unknown";
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

    @Test
    void panacheEntityWithDifferentStrategyEmitsWarning() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), QUARKUS_PANACHE_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults())
                .anyMatch(r -> r.status() == ValidationResult.Status.WARNING
                        && r.evidence().contains("PanacheEntity uses default strategy AUTO, Spring used IDENTITY"));

        assertThat(report.getResults())
                .anyMatch(r -> r.status() == ValidationResult.Status.PASSED
                        && r.evidence().contains("Field 'id' inherited from PanacheEntity"));

        assertThat(report.getResults())
                .noneMatch(r -> r.status() == ValidationResult.Status.FAILED);
    }

    @Test
    void panacheEntityWithAutoStrategyPasses() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"), SPRING_ENTITY_AUTO_STRATEGY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), QUARKUS_PANACHE_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults())
                .anyMatch(r -> r.status() == ValidationResult.Status.PASSED
                        && r.evidence().contains("PanacheEntity"));

        assertThat(report.getResults())
                .noneMatch(r -> r.status() == ValidationResult.Status.FAILED
                        || r.status() == ValidationResult.Status.WARNING);
    }

    private static final String SPRING_ENTITY_AUTO_STRATEGY = """
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

    @Test
    void springIdInBaseEntityIsResolvedByInheritance() throws IOException {
        Path springProject = createProject(tempDir.resolve("spring"),
                SPRING_BASE_ENTITY, SPRING_ENTITY_EXTENDS_BASE);
        Path quarkusProject = createProject(tempDir.resolve("quarkus"), SPRING_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(new TreeSitterAnalyzer(), springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults())
                .noneMatch(r -> r.status() == ValidationResult.Status.FAILED
                        && r.evidence().contains("ID strategy"));
    }

    private static final String SPRING_BASE_ENTITY = """
            package com.example;

            import jakarta.persistence.*;

            @MappedSuperclass
            public abstract class BaseEntity {
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                private Long id;

                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            }
            """;

    private static final String SPRING_ENTITY_EXTENDS_BASE = """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            @Table(name = "todos")
            public class Todo extends BaseEntity {

                @Column(nullable = false)
                private String title;

                private String description;

                private boolean completed;
            }
            """;

    private static final String QUARKUS_PANACHE_ENTITY = """
            package com.example;

            import jakarta.persistence.*;
            import io.quarkus.hibernate.orm.panache.PanacheEntity;

            @Entity
            @Table(name = "todos")
            public class Todo extends PanacheEntity {

                @Column(nullable = false)
                public String title;

                public String description;

                public boolean completed;
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