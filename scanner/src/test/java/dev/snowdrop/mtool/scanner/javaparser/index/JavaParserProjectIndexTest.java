package dev.snowdrop.mtool.scanner.javaparser.index;

import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import dev.snowdrop.mtool.scanner.fluent.FieldResult;
import dev.snowdrop.mtool.scanner.fluent.MethodResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaParserProjectIndexTest {

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
            @Table(name = "orders")
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

    // --- Index construction ---

    @Test
    void buildIndex_allTypesIndexed() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE));

        assertEquals(4, index.allTypes().size());
        assertNotNull(index.getType("com.example.BaseEntity"));
        assertNotNull(index.getType("com.example.Order"));
        assertNotNull(index.getType("com.example.Product"));
        assertNotNull(index.getType("com.example.Auditable"));
        assertNull(index.getType("com.example.NonExistent"));
    }

    // --- Navigation ---

    @Test
    void getSuperclass_resolvesChain() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        JavaTypeDeclaration order = index.getType("com.example.Order");
        JavaTypeDeclaration superclass = index.getSuperclass(order);
        assertNotNull(superclass);
        assertEquals("BaseEntity", superclass.simpleName());

        assertNull(index.getSuperclass(superclass));
    }

    @Test
    void getSuperclassChain() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER));

        List<JavaTypeDeclaration> chain = index.getSuperclassChain(
                index.getType("com.example.Order"));
        assertEquals(1, chain.size());
        assertEquals("BaseEntity", chain.get(0).simpleName());
    }

    @Test
    void getSubclasses() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        List<JavaTypeDeclaration> subclasses = index.getSubclasses(
                index.getType("com.example.BaseEntity"));
        assertEquals(2, subclasses.size());
        assertTrue(subclasses.stream().anyMatch(t -> "Order".equals(t.simpleName())));
        assertTrue(subclasses.stream().anyMatch(t -> "Product".equals(t.simpleName())));
    }

    @Test
    void getImplementors() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(ORDER, AUDITABLE));

        List<JavaTypeDeclaration> implementors = index.getImplementors("com.example.Auditable");
        assertEquals(1, implementors.size());
        assertEquals("Order", implementors.get(0).simpleName());
    }

    // --- Incremental updates ---

    @Test
    void addOrUpdate_replacesExisting() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(List.of(ORDER));
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

    @Test
    void remove_removesType() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(ORDER, PRODUCT));
        assertEquals(2, index.allTypes().size());

        index.remove("Order.java");
        assertEquals(1, index.allTypes().size());
        assertNull(index.getType("com.example.Order"));
        assertNotNull(index.getType("com.example.Product"));
    }

    // --- Fluent TypeQuery ---

    @Test
    void findTypes_annotatedWith() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE));

        List<JavaTypeDeclaration> entities = index.findTypes()
                .annotatedWith("Entity")
                .toList();

        assertEquals(2, entities.size());
        assertTrue(entities.stream().anyMatch(t -> "Order".equals(t.simpleName())));
        assertTrue(entities.stream().anyMatch(t -> "Product".equals(t.simpleName())));
    }

    @Test
    void findTypes_annotatedWithFqn() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE));

        List<JavaTypeDeclaration> entities = index.findTypes()
                .annotatedWithFqn("jakarta.persistence.Entity")
                .toList();

        assertEquals(2, entities.size());
        assertTrue(entities.stream().anyMatch(t -> "Order".equals(t.simpleName())));
        assertTrue(entities.stream().anyMatch(t -> "Product".equals(t.simpleName())));

        assertEquals(0, index.findTypes()
                .annotatedWithFqn("javax.persistence.Entity")
                .count());
    }

    @Test
    void findTypes_annotatedWithArgument() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(ORDER, PRODUCT));

        List<JavaTypeDeclaration> result = index.findTypes()
                .annotatedWith("Table", "name", "\"orders\"")
                .toList();

        assertEquals(1, result.size());
        assertEquals("Order", result.get(0).simpleName());
    }

    @Test
    void findTypes_extending() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE));

        List<JavaTypeDeclaration> result = index.findTypes()
                .extending("BaseEntity")
                .toList();

        assertEquals(2, result.size());
    }

    @Test
    void findTypes_compoundQuery() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT, AUDITABLE));

        List<JavaTypeDeclaration> result = index.findTypes()
                .annotatedWith("Entity")
                .extending("BaseEntity")
                .implementing("Auditable")
                .toList();

        assertEquals(1, result.size());
        assertEquals("Order", result.get(0).simpleName());
    }

    @Test
    void findTypes_ofKindAndPackage() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, AUDITABLE));

        assertEquals(2, index.findTypes()
                .ofKind(JavaTypeKind.CLASS)
                .inPackage("com.example")
                .count());

        assertTrue(index.findTypes()
                .ofKind(JavaTypeKind.INTERFACE)
                .exists());
    }

    @Test
    void findTypes_withModifier() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        List<JavaTypeDeclaration> result = index.findTypes()
                .withModifier(JavaModifier.ABSTRACT)
                .toList();

        assertEquals(1, result.size());
        assertEquals("BaseEntity", result.get(0).simpleName());
    }

    // --- Fluent FieldQuery ---

    @Test
    void findFields_annotatedWith() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        List<FieldResult> idFields = index.findFields()
                .annotatedWith("Id")
                .toList();

        assertEquals(1, idFields.size());
        assertEquals("id", idFields.get(0).field().name());
        assertEquals("BaseEntity", idFields.get(0).declaringType().simpleName());
    }

    @Test
    void findFields_annotatedWithFqn() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        List<FieldResult> idFields = index.findFields()
                .annotatedWithFqn("jakarta.persistence.Id")
                .toList();

        assertEquals(1, idFields.size());
        assertEquals("id", idFields.get(0).field().name());

        assertEquals(0, index.findFields()
                .annotatedWithFqn("javax.persistence.Id")
                .count());
    }

    @Test
    void findFields_ofType() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(ORDER, PRODUCT));

        List<FieldResult> result = index.findFields()
                .ofType("String")
                .toList();

        assertEquals(2, result.size());
    }

    // --- Fluent MethodQuery ---

    @Test
    void findMethods_named() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));

        List<MethodResult> result = index.findMethods()
                .named("getId")
                .toList();

        assertEquals(1, result.size());
        assertEquals("BaseEntity", result.get(0).declaringType().simpleName());
    }

    @Test
    void findMethods_returningType() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(ORDER, PRODUCT));

        List<MethodResult> result = index.findMethods()
                .returningType("void")
                .toList();

        assertEquals(1, result.size());
        assertEquals("setDescription", result.get(0).method().name());
    }

    // --- Batch operations ---

    @Test
    void addOrUpdate_batch() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(List.of(AUDITABLE));
        assertEquals(1, index.allTypes().size());

        index.addOrUpdate(List.of(ORDER, PRODUCT));
        assertEquals(3, index.allTypes().size());
    }

    @Test
    void remove_batch() {
        JavaParserProjectIndex index = JavaParserProjectIndex.build(
                List.of(BASE_ENTITY, ORDER, PRODUCT));
        assertEquals(3, index.allTypes().size());

        index.remove(List.of("Order.java", "Product.java"));
        assertEquals(1, index.allTypes().size());
    }
}
