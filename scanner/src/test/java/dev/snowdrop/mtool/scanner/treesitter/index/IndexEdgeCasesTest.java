package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import io.roastedroot.treesitter.TreeSitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexEdgeCasesTest {

    static final JavaSource STATUS_ENUM = new JavaSource("Status.java", """
            package com.example;

            public enum Status {
                ACTIVE, INACTIVE, PENDING;

                private String label;

                public String label() { return label; }
                public boolean isActive() { return this == ACTIVE; }
            }
            """);

    static final JavaSource MULTI_TYPE = new JavaSource("Models.java", """
            package com.example;

            public class Outer {
            }

            class PackagePrivate {
                private int value;
            }
            """);

    static final JavaSource INTERFACE_HIERARCHY = new JavaSource("Repo.java", """
            package com.example;

            public interface Repository {
                void save(Object entity);
                Object findById(Long id);
            }
            """);

    static final JavaSource IMPL = new JavaSource("RepoImpl.java", """
            package com.example;

            public class RepositoryImpl implements Repository {
                public void save(Object entity) {}
                public Object findById(Long id) { return null; }
            }
            """);

    static final JavaSource NO_PACKAGE = new JavaSource("Scratch.java", """
            public class Scratch {
                private String data;
                public String getData() { return data; }
            }
            """);

    // --- Enum support ---

    @Test
    void enum_fieldsAndMethods() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(STATUS_ENUM))) {

            JavaTypeDeclaration status = index.getType("com.example.Status");
            assertNotNull(status);
            assertEquals(JavaTypeKind.ENUM, status.kind());
            assertEquals(1, status.fields().size());
            assertEquals("label", status.fields().get(0).name());
            assertEquals(2, status.methods().size());
        }
    }

    @Test
    void enum_fluent_ofKind() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(STATUS_ENUM, INTERFACE_HIERARCHY, IMPL))) {

            assertEquals(1, index.findTypes()
                    .ofKind(JavaTypeKind.ENUM)
                    .count());
        }
    }

    // --- Multiple types per file ---

    @Test
    void multiType_allIndexed() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(MULTI_TYPE))) {

            assertEquals(2, index.allTypes().size());
            assertNotNull(index.getType("com.example.Outer"));
            assertNotNull(index.getType("com.example.PackagePrivate"));
        }
    }

    @Test
    void multiType_removeByPath_removesAll() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(MULTI_TYPE, STATUS_ENUM))) {

            assertEquals(3, index.allTypes().size());
            index.remove("Models.java");
            assertEquals(1, index.allTypes().size());
            assertNotNull(index.getType("com.example.Status"));
        }
    }

    // --- No package ---

    @Test
    void noPackage_defaultPackage() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(NO_PACKAGE))) {

            JavaTypeDeclaration scratch = index.getType("Scratch");
            assertNotNull(scratch);
            assertEquals("", scratch.packageName());
            assertEquals("Scratch", scratch.fqn());
            assertEquals(1, scratch.fields().size());
            assertEquals(1, scratch.methods().size());
        }
    }

    // --- Same-package navigation ---

    @Test
    void samePackage_getImplementors() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(INTERFACE_HIERARCHY, IMPL))) {

            List<JavaTypeDeclaration> implementors = index.getImplementors("com.example.Repository");
            assertEquals(1, implementors.size());
            assertEquals("RepositoryImpl", implementors.get(0).simpleName());
        }
    }

    @Test
    void samePackage_getSuperclass() {
        JavaSource child = new JavaSource("Child.java", """
                package com.example;
                public class Child extends RepositoryImpl {}
                """);

        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(IMPL, child))) {

            JavaTypeDeclaration childType = index.getType("com.example.Child");
            JavaTypeDeclaration parent = index.getSuperclass(childType);
            assertNotNull(parent);
            assertEquals("RepositoryImpl", parent.simpleName());
        }
    }

    // --- Batch incremental operations ---

    @Test
    void addOrUpdate_batch() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(STATUS_ENUM))) {

            assertEquals(1, index.allTypes().size());

            index.addOrUpdate(List.of(INTERFACE_HIERARCHY, IMPL));
            assertEquals(3, index.allTypes().size());
            assertNotNull(index.getType("com.example.Repository"));
            assertNotNull(index.getType("com.example.RepositoryImpl"));
        }
    }

    @Test
    void remove_batch() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(STATUS_ENUM, INTERFACE_HIERARCHY, IMPL))) {

            assertEquals(3, index.allTypes().size());

            index.remove(List.of("Repo.java", "RepoImpl.java"));
            assertEquals(1, index.allTypes().size());
            assertNotNull(index.getType("com.example.Status"));
        }
    }

    @Test
    void addOrUpdate_replacesThenNavigates() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(INTERFACE_HIERARCHY, IMPL))) {

            assertEquals(1, index.getImplementors("com.example.Repository").size());

            JavaSource updatedImpl = new JavaSource("RepoImpl.java", """
                    package com.example;
                    public class RepositoryImpl {
                        public void doSomething() {}
                    }
                    """);
            index.addOrUpdate(updatedImpl);

            assertEquals(0, index.getImplementors("com.example.Repository").size());
            assertEquals(1, index.findMethods().named("doSomething").count());
        }
    }

    // --- Empty index ---

    @Test
    void emptyIndex_returnsEmpty() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of())) {

            assertEquals(0, index.allTypes().size());
            assertNull(index.getType("com.example.Anything"));
            assertEquals(0, index.findTypes().count());
            assertEquals(0, index.findFields().count());
            assertEquals(0, index.findMethods().count());
        }
    }

    // --- queryAll with position data ---

    @Test
    void queryAll_capturesPositionData() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(INTERFACE_HIERARCHY))) {

            List<IndexedQueryResult> results = index.queryAll(
                    "(method_declaration name: (identifier) @name)");

            assertEquals(2, results.size());
            IndexedQueryResult save = results.stream()
                    .filter(r -> "save".equals(r.text()))
                    .findFirst()
                    .orElseThrow();

            assertEquals("name", save.captureName());
            assertTrue(save.startRow() >= 0);
            assertTrue(save.startColumn() >= 0);
            assertNotNull(save.declaringType());
            assertEquals("Repository", save.declaringType().simpleName());
        }
    }

    @Test
    void queryAll_fieldDeclarations() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(STATUS_ENUM, NO_PACKAGE))) {

            List<IndexedQueryResult> results = index.queryAll(
                    "(field_declaration type: (_) @field_type)");

            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(r -> "String".equals(r.text())));
        }
    }
}
