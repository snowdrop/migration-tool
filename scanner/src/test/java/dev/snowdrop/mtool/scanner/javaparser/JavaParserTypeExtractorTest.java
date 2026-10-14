package dev.snowdrop.mtool.scanner.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import dev.snowdrop.mtool.scanner.javaparser.index.JavaParserTypeExtractor;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaParserTypeExtractorTest {

    @Test
    void extract_symbols() {
        // @formatter:off
        JavaSource source = new JavaSource("Order.java", """
                package com.example;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "orders")
                public class Order {
                    @Id
                    private Long id;
                    private String description;

                    public Long getId() {
                        return id;
                    }

                    public void setId(Long id) {
                        this.id = id;
                    }

                    public String getDescription() { return description; }

                    public int getDescriptionLength() { return getDescription().length(); }
                }
                """);
        // @formatter:on

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(new StringReader(source.sourceCode()));
        assertTrue(result.isSuccessful());
        CompilationUnit cu = result.getResult().get();

        ClassOrInterfaceDeclaration orderClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
        assertEquals("Order", orderClass.getNameAsString());

        NormalAnnotationExpr tableAnn = orderClass.findFirst(NormalAnnotationExpr.class,
                a -> "Table".equals(a.getNameAsString())).orElseThrow();
        assertEquals("\"orders\"", tableAnn.getPairs().stream()
                .filter(p -> "name".equals(p.getNameAsString()))
                .findFirst()
                .orElseThrow()
                .getValue()
                .toString());

        List<MethodDeclaration> methods = orderClass.findAll(MethodDeclaration.class);
        assertEquals(4, methods.size());

        MethodDeclaration getDesc = methods.stream()
                .filter(m -> "getDescription".equals(m.getNameAsString()))
                .findFirst()
                .orElseThrow();
        assertEquals("String", getDesc.getTypeAsString());

        MethodDeclaration getDescLength = methods.stream()
                .filter(m -> "getDescriptionLength".equals(m.getNameAsString()))
                .findFirst()
                .orElseThrow();
        assertEquals("int", getDescLength.getTypeAsString());

        List<MethodCallExpr> calls = getDescLength.findAll(MethodCallExpr.class);
        assertEquals(2, calls.size());
        assertTrue(calls.stream().anyMatch(c -> "getDescription".equals(c.getNameAsString())));
        assertTrue(calls.stream().anyMatch(c -> "length".equals(c.getNameAsString())));
    }

    @Test
    void extractClass_basic() {
        JavaSource source = new JavaSource("Order.java", """
                package com.example;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "orders")
                public class Order {
                    private String description;

                    public String getDescription() { return description; }
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        assertEquals(1, types.size());

        JavaTypeDeclaration order = types.get(0);
        assertEquals("Order", order.simpleName());
        assertEquals("com.example", order.packageName());
        assertEquals("com.example.Order", order.fqn());
        assertEquals(JavaTypeKind.CLASS, order.kind());
        assertTrue(order.modifiers().contains(JavaModifier.PUBLIC));
        assertEquals(2, order.annotations().size());
        assertEquals(1, order.fields().size());
        assertEquals("description", order.fields().get(0).name());
        assertEquals("String", order.fields().get(0).typeName());
        assertEquals(1, order.methods().size());
        assertEquals("getDescription", order.methods().get(0).name());
    }

    @Test
    void extractInterface() {
        JavaSource source = new JavaSource("Auditable.java", """
                package com.example;

                public interface Auditable {
                    default void audit() {}
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        assertEquals(1, types.size());
        assertEquals(JavaTypeKind.INTERFACE, types.get(0).kind());
        assertEquals("Auditable", types.get(0).simpleName());
        assertEquals(1, types.get(0).methods().size());
    }

    @Test
    void extractEnum_withMethods() {
        JavaSource source = new JavaSource("Status.java", """
                package com.example;

                public enum Status {
                    ACTIVE, INACTIVE;

                    private String label;

                    public String label() { return label; }
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        assertEquals(1, types.size());

        JavaTypeDeclaration status = types.get(0);
        assertEquals(JavaTypeKind.ENUM, status.kind());
        assertEquals(1, status.fields().size());
        assertEquals("label", status.fields().get(0).name());
        assertEquals(1, status.methods().size());
    }

    @Test
    void extractAnnotationArguments() {
        JavaSource source = new JavaSource("Order.java", """
                package com.example;

                import jakarta.persistence.Table;

                @Table(name = "orders", schema = "public")
                public class Order {}
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        var ann = types.get(0).annotations().get(0);
        assertEquals("Table", ann.name());
        assertTrue(ann.hasArgument("name", "\"orders\""));
        assertTrue(ann.hasArgument("schema", "\"public\""));
    }

    @Test
    void extractSuperclassAndInterfaces() {
        JavaSource source = new JavaSource("Order.java", """
                package com.example;

                public class Order extends BaseEntity implements Auditable, Serializable {
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        JavaTypeDeclaration order = types.get(0);
        assertEquals("BaseEntity", order.superclass());
        assertEquals(2, order.interfaces().size());
        assertTrue(order.interfaces().contains("Auditable"));
        assertTrue(order.interfaces().contains("Serializable"));
    }

    @Test
    void extractMethodParameters() {
        JavaSource source = new JavaSource("Service.java", """
                package com.example;

                public class Service {
                    public void process(String name, int count) {}
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        var method = types.get(0).methods().get(0);
        assertEquals("process", method.name());
        assertEquals("void", method.returnType());
        assertEquals(2, method.parameters().size());
        assertEquals("name", method.parameters().get(0).name());
        assertEquals("String", method.parameters().get(0).typeName());
        assertEquals("count", method.parameters().get(1).name());
        assertEquals("int", method.parameters().get(1).typeName());
    }

    @Test
    void extractMultipleTypes() {
        JavaSource source = new JavaSource("Models.java", """
                package com.example;

                public class Outer {}
                class PackagePrivate {
                    private int value;
                }
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        assertEquals(2, types.size());
    }

    @Test
    void extractImportMap() {
        JavaSource source = new JavaSource("Order.java", """
                package com.example;

                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;

                @Entity
                public class Order {}
                """);

        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        assertEquals("jakarta.persistence.Entity", types.get(0).resolveType("Entity"));
        assertEquals("jakarta.persistence.Table", types.get(0).resolveType("Table"));
    }
}
