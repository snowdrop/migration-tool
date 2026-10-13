package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import io.roastedroot.treesitter.TreeSitter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FluentQueryTest {

    static final JavaSource SERVICE = new JavaSource("UserService.java", """
            package com.app.service;

            import jakarta.inject.Singleton;
            import jakarta.transaction.Transactional;

            @Singleton
            public class UserService {
                private String serviceName;

                @Transactional
                public void createUser(String name) {}

                @Transactional
                public void deleteUser(Long id) {}

                public String findUser(Long id) { return null; }

                private void validate(String name) {}
            }
            """);

    static final JavaSource REPOSITORY = new JavaSource("UserRepository.java", """
            package com.app.repository;

            import jakarta.inject.Singleton;

            @Singleton
            public abstract class UserRepository {
                private Long lastId;
                private static final String TABLE = "users";

                public Long nextId() { return lastId++; }
                protected void flush() {}
            }
            """);

    static final JavaSource CONTROLLER = new JavaSource("UserController.java", """
            package com.app.web;

            import jakarta.ws.rs.Path;
            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.POST;

            @Path("/users")
            public class UserController {
                private final String basePath;

                @GET
                public String listUsers() { return "[]"; }

                @POST
                public void createUser(String body) {}
            }
            """);

    static final JavaSource HEALTH = new JavaSource("HealthCheck.java", """
            package com.app.web;

            public interface HealthCheck {
                default boolean isHealthy() { return true; }
            }
            """);

    // --- TypeQuery: withModifier ---

    @Test
    void findTypes_withModifier_abstract() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER, HEALTH))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .withModifier(JavaModifier.ABSTRACT)
                    .toList();

            assertEquals(1, result.size());
            assertEquals("UserRepository", result.get(0).simpleName());
        }
    }

    @Test
    void findTypes_withModifier_public() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER, HEALTH))) {

            assertEquals(4, index.findTypes()
                    .withModifier(JavaModifier.PUBLIC)
                    .count());
        }
    }

    // --- TypeQuery: withField / withMethod ---

    @Test
    void findTypes_withField_byTypeName() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .withField(f -> "Long".equals(f.typeName()))
                    .toList();

            assertEquals(1, result.size());
            assertEquals("UserRepository", result.get(0).simpleName());
        }
    }

    @Test
    void findTypes_withMethod_predicate() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .withMethod(m -> "void".equals(m.returnType()) && m.parameters().size() == 1)
                    .toList();

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(t -> "UserService".equals(t.simpleName())));
            assertTrue(result.stream().anyMatch(t -> "UserController".equals(t.simpleName())));
        }
    }

    // --- TypeQuery: first() terminal ---

    @Test
    void findTypes_first_present() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, HEALTH))) {

            Optional<JavaTypeDeclaration> result = index.findTypes()
                    .ofKind(JavaTypeKind.INTERFACE)
                    .first();

            assertTrue(result.isPresent());
            assertEquals("HealthCheck", result.get().simpleName());
        }
    }

    @Test
    void findTypes_first_empty() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, CONTROLLER))) {

            Optional<JavaTypeDeclaration> result = index.findTypes()
                    .ofKind(JavaTypeKind.ENUM)
                    .first();

            assertTrue(result.isEmpty());
        }
    }

    // --- TypeQuery: exists() / count() on empty ---

    @Test
    void findTypes_exists_false() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, CONTROLLER))) {

            assertFalse(index.findTypes()
                    .annotatedWith("Entity")
                    .exists());
        }
    }

    @Test
    void findTypes_count_zero() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE))) {

            assertEquals(0, index.findTypes()
                    .extending("SomeClass")
                    .count());
        }
    }

    // --- TypeQuery: inPackage prefix matching ---

    @Test
    void findTypes_inPackage_prefix() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER, HEALTH))) {

            assertEquals(2, index.findTypes()
                    .inPackage("com.app.web")
                    .count());

            assertEquals(4, index.findTypes()
                    .inPackage("com.app")
                    .count());
        }
    }

    // --- FieldQuery: ofType ---

    @Test
    void findFields_ofType() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<FieldResult> result = index.findFields()
                    .ofType("String")
                    .toList();

            assertEquals(3, result.size());
        }
    }

    // --- FieldQuery: withModifier ---

    @Test
    void findFields_withModifier_private() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<FieldResult> result = index.findFields()
                    .withModifier(JavaModifier.PRIVATE)
                    .toList();

            assertTrue(result.size() >= 3);
        }
    }

    @Test
    void findFields_withModifier_static() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY))) {

            List<FieldResult> result = index.findFields()
                    .withModifier(JavaModifier.STATIC)
                    .toList();

            assertEquals(1, result.size());
            assertEquals("TABLE", result.get(0).field().name());
            assertEquals("UserRepository", result.get(0).declaringType().simpleName());
        }
    }

    // --- FieldQuery: compound ---

    @Test
    void findFields_compound_typeAndModifier() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<FieldResult> result = index.findFields()
                    .ofType("String")
                    .withModifier(JavaModifier.STATIC)
                    .toList();

            assertEquals(1, result.size());
            assertEquals("TABLE", result.get(0).field().name());
        }
    }

    // --- MethodQuery: annotatedWith ---

    @Test
    void findMethods_annotatedWith() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, CONTROLLER))) {

            List<MethodResult> result = index.findMethods()
                    .annotatedWith("Transactional")
                    .toList();

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(
                    mr -> "UserService".equals(mr.declaringType().simpleName())));
        }
    }

    // --- MethodQuery: withModifier ---

    @Test
    void findMethods_withModifier_private() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY))) {

            List<MethodResult> result = index.findMethods()
                    .withModifier(JavaModifier.PRIVATE)
                    .toList();

            assertEquals(1, result.size());
            assertEquals("validate", result.get(0).method().name());
        }
    }

    @Test
    void findMethods_withModifier_protected() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY))) {

            List<MethodResult> result = index.findMethods()
                    .withModifier(JavaModifier.PROTECTED)
                    .toList();

            assertEquals(1, result.size());
            assertEquals("flush", result.get(0).method().name());
        }
    }

    // --- MethodQuery: inType ---

    @Test
    void findMethods_inType() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, REPOSITORY, CONTROLLER))) {

            List<MethodResult> result = index.findMethods()
                    .inType(t -> t.packageName().equals("com.app.web"))
                    .toList();

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(mr -> "listUsers".equals(mr.method().name())));
            assertTrue(result.stream().anyMatch(mr -> "createUser".equals(mr.method().name())));
        }
    }

    // --- MethodQuery: compound ---

    @Test
    void findMethods_compound_annotatedAndReturning() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(SERVICE, CONTROLLER))) {

            List<MethodResult> result = index.findMethods()
                    .annotatedWith("GET")
                    .returningType("String")
                    .toList();

            assertEquals(1, result.size());
            assertEquals("listUsers", result.get(0).method().name());
        }
    }
}
