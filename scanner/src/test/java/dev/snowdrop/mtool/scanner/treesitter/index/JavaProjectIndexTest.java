package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import io.roastedroot.treesitter.TreeSitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaProjectIndexTest {

    static final JavaSource BASE_ENTITY = new JavaSource("BaseEntity.java", """
            package com.example;

            import jakarta.persistence.MappedSuperclass;
            import jakarta.persistence.Id;
            import jakarta.persistence.GeneratedValue;

            @MappedSuperclass
            public abstract class BaseEntity {
                @Id
                @GeneratedValue
                private Long id;

                public Long getId() { return id; }
            }
            """);

    static final JavaSource ORDER = new JavaSource("Order.java", """
            package com.example;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Table;
            import jakarta.persistence.Column;

            @Entity
            @Table(name = TableNames.ORDERS)
            public class Order extends BaseEntity implements Auditable {
                @Column(name = "description", nullable = false)
                private String description;

                public String getDescription() { return description; }
                public void setDescription(String desc) { this.description = desc; }
            }
            """);

    static final JavaSource PRODUCT = new JavaSource("Product.java", """
            package com.example;

            import jakarta.persistence.Entity;

            @Entity
            public class Product extends BaseEntity {
                private String name;
                private double price;

                public String getName() { return name; }
            }
            """);

    static final JavaSource AUDITABLE = new JavaSource("Auditable.java", """
            package com.example;

            public interface Auditable {
                default void audit() {}
            }
            """);

    // --- Index construction and direct lookups ---

    @Test
    void buildIndex_allTypesIndexed() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE))) {

            assertEquals(4, index.allTypes().size());
            assertNotNull(index.getType("com.example.BaseEntity"));
            assertNotNull(index.getType("com.example.Order"));
            assertNotNull(index.getType("com.example.Product"));
            assertNotNull(index.getType("com.example.Auditable"));
            assertNull(index.getType("com.example.NonExistent"));
        }
    }

    // --- Navigation ---

    @Test
    void getSuperclass_resolvesChain() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT))) {

            JavaTypeDeclaration order = index.getType("com.example.Order");
            JavaTypeDeclaration superclass = index.getSuperclass(order);
            assertNotNull(superclass);
            assertEquals("BaseEntity", superclass.simpleName());

            assertNull(index.getSuperclass(superclass));
        }
    }

    @Test
    void getSuperclassChain() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER))) {

            List<JavaTypeDeclaration> chain = index.getSuperclassChain(
                    index.getType("com.example.Order"));
            assertEquals(1, chain.size());
            assertEquals("BaseEntity", chain.get(0).simpleName());
        }
    }

    @Test
    void getSubclasses() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT))) {

            List<JavaTypeDeclaration> subclasses = index.getSubclasses(
                    index.getType("com.example.BaseEntity"));
            assertEquals(2, subclasses.size());
            assertTrue(subclasses.stream().anyMatch(t -> "Order".equals(t.simpleName())));
            assertTrue(subclasses.stream().anyMatch(t -> "Product".equals(t.simpleName())));
        }
    }

    @Test
    void getImplementors() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, AUDITABLE))) {

            List<JavaTypeDeclaration> implementors = index.getImplementors("com.example.Auditable");
            assertEquals(1, implementors.size());
            assertEquals("Order", implementors.get(0).simpleName());
        }
    }

    // --- Incremental updates ---

    @Test
    void addOrUpdate_replacesExisting() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts, List.of(ORDER))) {
            assertEquals(1, index.allTypes().size());

            JavaSource updatedOrder = new JavaSource("Order.java", """
                    package com.example;
                    public class Order {
                        private int quantity;
                    }
                    """);
            index.addOrUpdate(updatedOrder);

            assertEquals(1, index.allTypes().size());
            JavaTypeDeclaration order = index.getType("com.example.Order");
            assertEquals(1, order.fields().size());
            assertEquals("quantity", order.fields().get(0).name());
        }
    }

    @Test
    void remove_removesType() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, PRODUCT))) {
            assertEquals(2, index.allTypes().size());

            index.remove("Order.java");
            assertEquals(1, index.allTypes().size());
            assertNull(index.getType("com.example.Order"));
            assertNotNull(index.getType("com.example.Product"));
        }
    }

    // --- Fluent TypeQuery ---

    @Test
    void findTypes_annotatedWith() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE))) {

            List<JavaTypeDeclaration> entities = index.findTypes()
                    .annotatedWith("Entity")
                    .toList();

            assertEquals(2, entities.size());
            assertTrue(entities.stream().anyMatch(t -> "Order".equals(t.simpleName())));
            assertTrue(entities.stream().anyMatch(t -> "Product".equals(t.simpleName())));
        }
    }

    @Test
    void findTypes_annotatedWithArgument() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, PRODUCT))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .annotatedWith("Table", "name", "TableNames.ORDERS")
                    .toList();

            assertEquals(1, result.size());
            assertEquals("Order", result.get(0).simpleName());
        }
    }

    @Test
    void findTypes_extending() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .extending("BaseEntity")
                    .toList();

            assertEquals(2, result.size());
        }
    }

    @Test
    void findTypes_compoundQuery() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE))) {

            List<JavaTypeDeclaration> result = index.findTypes()
                    .annotatedWith("Entity")
                    .extending("BaseEntity")
                    .implementing("Auditable")
                    .toList();

            assertEquals(1, result.size());
            assertEquals("Order", result.get(0).simpleName());
        }
    }

    @Test
    void findTypes_ofKindAndPackage() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, AUDITABLE))) {

            assertEquals(2, index.findTypes()
                    .ofKind(JavaTypeKind.CLASS)
                    .inPackage("com.example")
                    .count());

            assertTrue(index.findTypes()
                    .ofKind(JavaTypeKind.INTERFACE)
                    .exists());
        }
    }

    // --- Fluent FieldQuery ---

    @Test
    void findFields_annotatedWith() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT))) {

            List<FieldResult> idFields = index.findFields()
                    .annotatedWith("Id")
                    .toList();

            assertEquals(1, idFields.size());
            assertEquals("id", idFields.get(0).field().name());
            assertEquals("BaseEntity", idFields.get(0).declaringType().simpleName());
        }
    }

    @Test
    void findFields_annotatedWithArgument() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, PRODUCT))) {

            int count = index.findFields()
                    .annotatedWith("Column", "nullable", "false")
                    .count();

            assertEquals(1, count);
        }
    }

    @Test
    void findFields_inType() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, PRODUCT))) {

            List<FieldResult> result = index.findFields()
                    .inType(t -> "Order".equals(t.simpleName()))
                    .toList();

            assertEquals(1, result.size());
            assertEquals("description", result.get(0).field().name());
        }
    }

    // --- Fluent MethodQuery ---

    @Test
    void findMethods_named() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT))) {

            List<MethodResult> result = index.findMethods()
                    .named("getId")
                    .toList();

            assertEquals(1, result.size());
            assertEquals("BaseEntity", result.get(0).declaringType().simpleName());
        }
    }

    @Test
    void findMethods_returningType() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(ORDER, PRODUCT))) {

            List<MethodResult> result = index.findMethods()
                    .returningType("void")
                    .toList();

            assertEquals(1, result.size());
            assertEquals("setDescription", result.get(0).method().name());
        }
    }

    // --- queryAll ---

    @Test
    void queryAll_findsAcrossFiles() {
        try (var ts = TreeSitter.create();
                var index = JavaProjectIndex.build(ts,
                        List.of(BASE_ENTITY, ORDER, PRODUCT))) {

            List<IndexedQueryResult> results = index.queryAll(
                    "(method_declaration name: (identifier) @method_name)");

            assertTrue(results.size() >= 4);
            assertTrue(results.stream().anyMatch(r -> "getId".equals(r.text())));
            assertTrue(results.stream().anyMatch(r -> "getDescription".equals(r.text())));
            assertTrue(results.stream().anyMatch(r -> "getName".equals(r.text())));
        }
    }
}
