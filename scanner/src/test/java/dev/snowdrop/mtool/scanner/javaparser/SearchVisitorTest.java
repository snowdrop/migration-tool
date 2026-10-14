package dev.snowdrop.mtool.scanner.javaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchVisitorTest {

    record MatchResult(boolean found, String kind, String name, int line, int column) {

        @Override
        public String toString() {
            return "%s %s: %s at L%d:C%d".formatted(found ? "FOUND" : "NOT_FOUND", kind, name, line, column);
        }
    }

    // @formatter:off
    static final String SOURCE = """
            package com.example;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Table;
            import jakarta.persistence.Id;
            import jakarta.persistence.GeneratedValue;

            @Entity
            @Table(name = "orders")
            public class Order {
                @Id
                @GeneratedValue
                private Long id;

                private String description;

                public Long getId() { return id; }

                public String getDescription() { return description; }

                public void setDescription(String desc) { this.description = desc; }
            }
            """;
    // @formatter:on

    @Test
    void searchImport() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<String>() {
            @Override
            public void visit(ImportDeclaration n, String target) {
                if (n.getNameAsString().equals(target)) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    int col = n.getBegin().map(p -> p.column).orElse(0);
                    results.add(new MatchResult(true, "import", target, line, col));
                }
                super.visit(n, target);
            }
        }, "jakarta.persistence.Entity");

        assertEquals(1, results.size());
        assertTrue(results.get(0).found());
        assertEquals("import", results.get(0).kind());
        assertTrue(results.get(0).line() > 0);

        results.forEach(System.out::println);
    }

    @Test
    void searchImport_notFound() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<String>() {
            @Override
            public void visit(ImportDeclaration n, String target) {
                if (n.getNameAsString().equals(target)) {
                    results.add(new MatchResult(true, "import", target,
                            n.getBegin().map(p -> p.line).orElse(0),
                            n.getBegin().map(p -> p.column).orElse(0)));
                }
                super.visit(n, target);
            }
        }, "org.springframework.beans.factory.annotation.Autowired");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchAnnotation() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                for (AnnotationExpr ann : n.getAnnotations()) {
                    if ("Entity".equals(ann.getNameAsString())) {
                        results.add(new MatchResult(true, "annotation", "@Entity",
                                ann.getBegin().map(p -> p.line).orElse(0),
                                ann.getBegin().map(p -> p.column).orElse(0)));
                    }
                }
                super.visit(n, arg);
            }
        }, null);

        assertEquals(1, results.size());
        assertTrue(results.get(0).found());
        assertEquals("@Entity", results.get(0).name());

        results.forEach(System.out::println);
    }

    @Test
    void searchAnnotationWithParameter() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(NormalAnnotationExpr n, Void arg) {
                if ("Table".equals(n.getNameAsString())) {
                    boolean hasParam = n.getPairs().stream()
                            .anyMatch(p -> "name".equals(p.getNameAsString())
                                    && "\"orders\"".equals(p.getValue().toString()));
                    if (hasParam) {
                        results.add(new MatchResult(true, "annotation", "@Table(name=\"orders\")",
                                n.getBegin().map(p -> p.line).orElse(0),
                                n.getBegin().map(p -> p.column).orElse(0)));
                    }
                }
                super.visit(n, arg);
            }
        }, null);

        assertEquals(1, results.size());
        assertTrue(results.get(0).found());
        assertEquals("annotation", results.get(0).kind());

        results.forEach(System.out::println);
    }

    @Test
    void searchAnnotationWithParameter_wrongValue() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(NormalAnnotationExpr n, Void arg) {
                if ("Table".equals(n.getNameAsString())) {
                    boolean hasParam = n.getPairs().stream()
                            .anyMatch(p -> "name".equals(p.getNameAsString())
                                    && "\"products\"".equals(p.getValue().toString()));
                    if (hasParam) {
                        results.add(new MatchResult(true, "annotation", "@Table(name=\"products\")",
                                n.getBegin().map(p -> p.line).orElse(0),
                                n.getBegin().map(p -> p.column).orElse(0)));
                    }
                }
                super.visit(n, arg);
            }
        }, null);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchMethod() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<String>() {
            @Override
            public void visit(MethodDeclaration n, String targetName) {
                if (n.getNameAsString().equals(targetName)) {
                    results.add(new MatchResult(true, "method", n.getNameAsString(),
                            n.getBegin().map(p -> p.line).orElse(0),
                            n.getBegin().map(p -> p.column).orElse(0)));
                }
                super.visit(n, targetName);
            }
        }, "getDescription");

        assertEquals(1, results.size());
        assertTrue(results.get(0).found());
        assertEquals("method", results.get(0).kind());

        results.forEach(System.out::println);
    }

    @Test
    void searchFieldByAnnotation() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(FieldDeclaration n, Void arg) {
                boolean hasId = n.getAnnotations().stream()
                        .anyMatch(a -> "Id".equals(a.getNameAsString()));
                if (hasId) {
                    String fieldName = n.getVariables().get(0).getNameAsString();
                    results.add(new MatchResult(true, "field", fieldName,
                            n.getBegin().map(p -> p.line).orElse(0),
                            n.getBegin().map(p -> p.column).orElse(0)));
                }
                super.visit(n, arg);
            }
        }, null);

        assertEquals(1, results.size());
        assertEquals("id", results.get(0).name());

        results.forEach(System.out::println);
    }

    @Test
    void searchMultipleAnnotations() {
        CompilationUnit cu = StaticJavaParser.parse(SOURCE);
        List<MatchResult> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<List<String>>() {
            @Override
            public void visit(FieldDeclaration n, List<String> targets) {
                for (AnnotationExpr ann : n.getAnnotations()) {
                    if (targets.contains(ann.getNameAsString())) {
                        String fieldName = n.getVariables().get(0).getNameAsString();
                        results.add(new MatchResult(true, "field-annotation",
                                fieldName + "/@" + ann.getNameAsString(),
                                ann.getBegin().map(p -> p.line).orElse(0),
                                ann.getBegin().map(p -> p.column).orElse(0)));
                    }
                }
                super.visit(n, targets);
            }
        }, List.of("Id", "GeneratedValue"));

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.name().contains("@Id")));
        assertTrue(results.stream().anyMatch(r -> r.name().contains("@GeneratedValue")));

        results.forEach(System.out::println);
    }
}
