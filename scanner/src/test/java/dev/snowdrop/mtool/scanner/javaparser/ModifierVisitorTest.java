package dev.snowdrop.mtool.scanner.javaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModifierVisitorTest {

    record ChangeMarker(int line, String kind, String before, String after) {

        @Override
        public String toString() {
            return "L%d ~~> %s: [%s] → [%s]".formatted(line, kind, before, after);
        }
    }

    @Test
    void replaceAutowiredWithInject() {
        // @formatter:off
        String source = """
                package com.example;

                import org.springframework.beans.factory.annotation.Autowired;

                public class OrderService {
                    @Autowired
                    private OrderRepository repository;

                    @Autowired
                    private NotificationService notificationService;
                }
                """;
        // @formatter:on

        CompilationUnit cu = StaticJavaParser.parse(source);
        List<ChangeMarker> changes = new ArrayList<>();

        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(MarkerAnnotationExpr n, Void arg) {
                if ("Autowired".equals(n.getNameAsString())) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    changes.add(new ChangeMarker(line, "annotation", "@Autowired", "@Inject"));
                    return new MarkerAnnotationExpr(new Name("Inject"));
                }
                return super.visit(n, arg);
            }

            @Override
            public Node visit(ImportDeclaration n, Void arg) {
                if ("org.springframework.beans.factory.annotation.Autowired".equals(n.getNameAsString())) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    changes.add(new ChangeMarker(line, "import",
                            "org.springframework.beans.factory.annotation.Autowired",
                            "jakarta.inject.Inject"));
                    return new ImportDeclaration("jakarta.inject.Inject", false, false);
                }
                return super.visit(n, arg);
            }
        }, null);

        assertEquals(3, changes.size());

        assertEquals("import", changes.get(0).kind());
        assertEquals("org.springframework.beans.factory.annotation.Autowired", changes.get(0).before());
        assertEquals("jakarta.inject.Inject", changes.get(0).after());

        assertEquals("annotation", changes.get(1).kind());
        assertEquals("@Autowired", changes.get(1).before());
        assertEquals("@Inject", changes.get(1).after());

        assertEquals("annotation", changes.get(2).kind());
        assertTrue(changes.get(2).line() > changes.get(1).line());

        changes.forEach(c -> System.out.println(c));
    }

    @Test
    void replaceAutowiredPreservesOtherAnnotations() {
        // @formatter:off
        String source = """
                package com.example;

                import org.springframework.beans.factory.annotation.Autowired;
                import jakarta.persistence.Entity;

                @Entity
                public class OrderService {
                    @Autowired
                    private OrderRepository repository;
                }
                """;
        // @formatter:on

        CompilationUnit cu = StaticJavaParser.parse(source);
        List<ChangeMarker> changes = new ArrayList<>();

        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(MarkerAnnotationExpr n, Void arg) {
                if ("Autowired".equals(n.getNameAsString())) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    changes.add(new ChangeMarker(line, "annotation", "@Autowired", "@Inject"));
                    return new MarkerAnnotationExpr(new Name("Inject"));
                }
                return super.visit(n, arg);
            }

            @Override
            public Node visit(ImportDeclaration n, Void arg) {
                if ("org.springframework.beans.factory.annotation.Autowired".equals(n.getNameAsString())) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    changes.add(new ChangeMarker(line, "import",
                            "org.springframework.beans.factory.annotation.Autowired",
                            "jakarta.inject.Inject"));
                    return new ImportDeclaration("jakarta.inject.Inject", false, false);
                }
                return super.visit(n, arg);
            }
        }, null);

        assertEquals(2, changes.size());
        assertTrue(changes.stream().noneMatch(c -> c.before().contains("Entity")));

        String result = cu.toString();
        assertTrue(result.contains("@Entity"));
        assertTrue(result.contains("import jakarta.persistence.Entity;"));

        changes.forEach(c -> System.out.println(c));
    }
}
