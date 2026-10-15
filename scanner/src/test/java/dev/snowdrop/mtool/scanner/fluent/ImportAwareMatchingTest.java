package dev.snowdrop.mtool.scanner.fluent;

import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.scanner.javaparser.index.JavaParserProjectIndex;
import dev.snowdrop.mtool.scanner.treesitter.index.JavaProjectIndex;
import io.roastedroot.treesitter.TreeSitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that annotations are matched on their simple name cross-checked with the imports
 * (explicit, wildcard, same package) and fully qualified names, whatever the parser used.
 */
class ImportAwareMatchingTest {

    private static final String ENTITY_FQN = "jakarta.persistence.Entity";

    static final JavaSource EXPLICIT_IMPORT = new JavaSource("Explicit.java", """
            package com.example;

            import jakarta.persistence.Entity;

            @Entity
            public class Explicit {}
            """);

    static final JavaSource WILDCARD_IMPORT = new JavaSource("Wildcard.java", """
            package com.example;

            import jakarta.persistence.*;

            @Entity
            public class Wildcard {}
            """);

    static final JavaSource FQN_IN_SOURCE = new JavaSource("Qualified.java", """
            package com.example;

            @jakarta.persistence.Entity
            public class Qualified {}
            """);

    static final JavaSource OTHER_PACKAGE_ENTITY = new JavaSource("Other.java", """
            package com.example;

            import com.other.Entity;

            @Entity
            public class Other {}
            """);

    static final JavaSource WILDCARD_OTHER_PACKAGE = new JavaSource("OtherWildcard.java", """
            package com.example;

            import com.other.*;

            @Entity
            public class OtherWildcard {}
            """);

    static final JavaSource EXPLICIT_WINS_OVER_WILDCARD = new JavaSource("Mixed.java", """
            package com.example;

            import jakarta.persistence.*;
            import com.other.Entity;

            @Entity
            public class Mixed {}
            """);

    static final JavaSource WILDCARD_ON_FIELD = new JavaSource("FieldWildcard.java", """
            package com.example;

            import jakarta.persistence.*;

            public class FieldWildcard {
                @Id
                private Long id;
            }
            """);

    static final List<JavaSource> ALL = List.of(EXPLICIT_IMPORT, WILDCARD_IMPORT, FQN_IN_SOURCE,
            OTHER_PACKAGE_ENTITY, WILDCARD_OTHER_PACKAGE, EXPLICIT_WINS_OVER_WILDCARD, WILDCARD_ON_FIELD);

    private static List<String> names(List<JavaTypeDeclaration> types) {
        return types.stream().map(JavaTypeDeclaration::simpleName).sorted().toList();
    }

    @Test
    void treeSitter_annotatedWithFqn_matchesExplicitWildcardAndQualified() {
        try (var ts = TreeSitter.create(); var index = JavaProjectIndex.build(ts, ALL)) {
            assertEquals(List.of("Explicit", "Qualified", "Wildcard"),
                    names(index.findTypes().annotatedWithFqn(ENTITY_FQN).toList()));
        }
    }

    @Test
    void javaParser_annotatedWithFqn_matchesExplicitWildcardAndQualified() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(ALL);
        assertEquals(List.of("Explicit", "Qualified", "Wildcard"),
                names(index.findTypes().annotatedWithFqn(ENTITY_FQN).toList()));
    }

    @Test
    void treeSitter_fieldAnnotatedWithFqn_matchesWildcard() {
        try (var ts = TreeSitter.create(); var index = JavaProjectIndex.build(ts, ALL)) {
            assertEquals(1, index.findFields().annotatedWithFqn("jakarta.persistence.Id").count());
        }
    }

    @Test
    void javaParser_fieldAnnotatedWithFqn_matchesWildcard() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(ALL);
        assertEquals(1, index.findFields().annotatedWithFqn("jakarta.persistence.Id").count());
    }

    @Test
    void wildcardImports_areExposedByBothParsers() {
        try (var ts = TreeSitter.create(); var index = JavaProjectIndex.build(ts, List.of(WILDCARD_IMPORT))) {
            assertEquals(List.of("jakarta.persistence"), index.getType("com.example.Wildcard").wildcardImports());
            assertEquals(java.util.Map.of(), index.getType("com.example.Wildcard").importMap());
        }
        JavaParserProjectIndex jp = JavaParserProjectIndex.build(List.of(WILDCARD_IMPORT));
        assertEquals(List.of("jakarta.persistence"), jp.getType("com.example.Wildcard").wildcardImports());
        assertEquals(java.util.Map.of(), jp.getType("com.example.Wildcard").importMap());
    }
}
