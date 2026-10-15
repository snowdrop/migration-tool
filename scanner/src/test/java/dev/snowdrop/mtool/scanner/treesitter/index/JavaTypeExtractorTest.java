package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import io.roastedroot.treesitter.TreeSitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeExtractorTest {

    @Test
    void extractClassWithInheritanceAndAnnotations() {
        String source = """
                package com.example;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = TableNames.ORDERS)
                public class Order extends BaseEntity implements Serializable, Auditable {

                    @Id
                    private Long id;

                    private String description;

                    @Override
                    public Long getId() { return id; }

                    public void setDescription(String desc) { this.description = desc; }
                }
                """;

        try (var ts = TreeSitter.create()) {
            List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts,
                    new JavaSource("src/main/java/com/example/Order.java", source));

            assertEquals(1, types.size());
            JavaTypeDeclaration order = types.get(0);

            assertEquals("com.example", order.packageName());
            assertEquals("Order", order.simpleName());
            assertEquals("com.example.Order", order.fqn());
            assertEquals(JavaTypeKind.CLASS, order.kind());
            assertTrue(order.modifiers().contains(JavaModifier.PUBLIC));

            assertEquals("BaseEntity", order.superclass());
            assertEquals(List.of("Serializable", "Auditable"), order.interfaces());

            // Import map
            assertEquals("jakarta.persistence.Entity", order.importMap().get("Entity"));
            assertEquals("jakarta.persistence.Table", order.importMap().get("Table"));

            // FQN resolution
            assertEquals("jakarta.persistence.Entity", order.resolveType("Entity"));
            assertEquals("BaseEntity", order.superclassFqn());
            assertEquals("BaseEntity", order.superclass());

            // Annotations
            assertEquals(2, order.annotations().size());
            assertEquals("Entity", order.annotations().get(0).name());
            assertEquals("Table", order.annotations().get(1).name());
            assertTrue(order.annotations().get(1).hasArgument("name", "TableNames.ORDERS"));

            // Fields
            assertEquals(2, order.fields().size());
            JavaField idField = order.fields().get(0);
            assertEquals("id", idField.name());
            assertEquals("Long", idField.typeName());
            assertTrue(idField.modifiers().contains(JavaModifier.PRIVATE));
            assertEquals(1, idField.annotations().size());
            assertEquals("Id", idField.annotations().get(0).name());

            JavaField descField = order.fields().get(1);
            assertEquals("description", descField.name());
            assertEquals("String", descField.typeName());
            assertTrue(descField.annotations().isEmpty());

            // Methods
            assertEquals(2, order.methods().size());
            JavaMethod getId = order.methods().get(0);
            assertEquals("getId", getId.name());
            assertEquals("Long", getId.returnType());
            assertTrue(getId.parameters().isEmpty());
            assertEquals(1, getId.annotations().size());
            assertEquals("Override", getId.annotations().get(0).name());

            JavaMethod setDesc = order.methods().get(1);
            assertEquals("setDescription", setDesc.name());
            assertEquals("void", setDesc.returnType());
            assertEquals(1, setDesc.parameters().size());
            assertEquals("desc", setDesc.parameters().get(0).name());
            assertEquals("String", setDesc.parameters().get(0).typeName());
            assertTrue(setDesc.modifiers().contains(JavaModifier.PUBLIC));
        }
    }

    @Test
    void extractInterface() {
        String source = """
                package com.example;

                public interface Auditable extends Trackable {
                    void audit();
                    String getAuditLog();
                }
                """;

        try (var ts = TreeSitter.create()) {
            List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts,
                    new JavaSource("Auditable.java", source));

            assertEquals(1, types.size());
            JavaTypeDeclaration iface = types.get(0);

            assertEquals("Auditable", iface.simpleName());
            assertEquals(JavaTypeKind.INTERFACE, iface.kind());
            assertEquals(List.of("Trackable"), iface.interfaces());
            assertNull(iface.superclass());
            assertEquals(2, iface.methods().size());
        }
    }

    @Test
    void extractEnum() {
        String source = """
                package com.example;

                public enum Status {
                    ACTIVE, INACTIVE;

                    public boolean isActive() { return this == ACTIVE; }
                }
                """;

        try (var ts = TreeSitter.create()) {
            List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts,
                    new JavaSource("Status.java", source));

            assertEquals(1, types.size());
            JavaTypeDeclaration enumType = types.get(0);

            assertEquals("Status", enumType.simpleName());
            assertEquals(JavaTypeKind.ENUM, enumType.kind());
            assertEquals(1, enumType.methods().size());
            assertEquals("isActive", enumType.methods().get(0).name());
        }
    }

    @Test
    void extractMultipleTypesInOneFile() {
        String source = """
                package com.example;

                public class Order {
                    private Long id;
                }

                interface Auditable {
                    void audit();
                }
                """;

        try (var ts = TreeSitter.create()) {
            List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts,
                    new JavaSource("Order.java", source));

            assertEquals(2, types.size());
            assertEquals("Order", types.get(0).simpleName());
            assertEquals(JavaTypeKind.CLASS, types.get(0).kind());
            assertEquals("Auditable", types.get(1).simpleName());
            assertEquals(JavaTypeKind.INTERFACE, types.get(1).kind());
        }
    }

    @Test
    void extractNoPackage() {
        String source = """
                class Simple {
                    int value;
                }
                """;

        try (var ts = TreeSitter.create()) {
            List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts,
                    new JavaSource("Simple.java", source));

            assertEquals(1, types.size());
            assertEquals("", types.get(0).packageName());
            assertEquals("Simple", types.get(0).simpleName());
            assertEquals("Simple", types.get(0).fqn());
        }
    }
}
