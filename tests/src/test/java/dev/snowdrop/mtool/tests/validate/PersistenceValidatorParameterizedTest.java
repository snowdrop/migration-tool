package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.scanner.analyzer.JavaAnalyzer;
import dev.snowdrop.mtool.scanner.analyzer.JavaParserAnalyzer;
import dev.snowdrop.mtool.scanner.analyzer.TreeSitterAnalyzer;
import dev.snowdrop.mtool.validate.persistence.PersistenceValidator;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceValidatorParameterizedTest {

    @TempDir
    Path tempDir;

    static Stream<JavaAnalyzer> analyzers() {
        return Stream.of(new TreeSitterAnalyzer(), new JavaParserAnalyzer());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void extractsEntityFromJavaSource(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve(analyzer.getClass().getSimpleName()), SPRING_ENTITY);

        AnalysisResult result = analyzer.analyze(springProject);

        assertThat(result.getSymbolTable()).isNotEmpty();
        assertThat(result.getSymbolTable().values().stream()
                .flatMap(cu -> cu.getTypeDeclarations().values().stream())
                .filter(t -> t.getAnnotations().stream().anyMatch(a -> a.contains("@Entity")))
                .count()).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void extractsTableName(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve(analyzer.getClass().getSimpleName()), SPRING_ENTITY);

        AnalysisResult result = analyzer.analyze(springProject);

        var types = result.getSymbolTable().values().iterator().next().getTypeDeclarations();
        var type = types.values().iterator().next();
        assertThat(type.getAnnotations()).anyMatch(a -> a.contains("Table") && a.contains("todos"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void extractsFieldsWithAnnotations(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve(analyzer.getClass().getSimpleName()), SPRING_ENTITY);

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void identicalProjectsPassValidation(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve("spring-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(analyzer, springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isZero();
        assertThat(report.overallStatus()).isEqualTo("PASS");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void detectsMissingEntity(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve("spring-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus-" + analyzer.getClass().getSimpleName()),
                DIFFERENT_ENTITY);

        PersistenceValidator validator = new PersistenceValidator(analyzer, springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.failed()).isGreaterThan(0);
        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing entities"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void detectsMissingField(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve("spring-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus-" + analyzer.getClass().getSimpleName()),
                ENTITY_MISSING_FIELD);

        PersistenceValidator validator = new PersistenceValidator(analyzer, springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("Missing field"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void detectsIdStrategyChange(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve("spring-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);
        Path quarkusProject = createProject(tempDir.resolve("quarkus-" + analyzer.getClass().getSimpleName()),
                ENTITY_DIFFERENT_ID_STRATEGY);

        PersistenceValidator validator = new PersistenceValidator(analyzer, springProject, true);
        ValidationReport report = validator.validate(quarkusProject);

        assertThat(report.getResults()).anyMatch(r -> r.status() == ValidationResult.Status.FAILED
                && r.evidence().contains("ID strategy changed"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("analyzers")
    void entityCountMismatchFails(JavaAnalyzer analyzer) throws IOException {
        Path springProject = createProject(tempDir.resolve("spring-" + analyzer.getClass().getSimpleName()), SPRING_ENTITY);
        Path quarkusProject = tempDir.resolve("quarkus-" + analyzer.getClass().getSimpleName());
        Files.createDirectories(quarkusProject.resolve("src/main/java"));

        PersistenceValidator validator = new PersistenceValidator(analyzer, springProject, true);
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

    // @formatter:off
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
    // @formatter:on
}
