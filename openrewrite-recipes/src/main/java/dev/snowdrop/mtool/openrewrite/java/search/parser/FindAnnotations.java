package dev.snowdrop.mtool.openrewrite.java.search.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FindAnnotations {

    public static final String SEARCH_MARKER = "/*~~>*/";

    public record AnnotationMatch(String annotationName, String enclosingClass, int line, int column) {
    }

    public static List<AnnotationMatch> find(String sourceCode, String annotationPattern) {
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);
        return collectMatches(cu, annotationPattern);
    }

    public static List<AnnotationMatch> find(Path sourceFile, String annotationPattern) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        return collectMatches(cu, annotationPattern);
    }

    public static String findAndMark(String sourceCode, String annotationPattern) {
        List<AnnotationMatch> matches = find(sourceCode, annotationPattern);
        if (matches.isEmpty()) {
            return sourceCode;
        }
        String[] lines = sourceCode.split("\n", -1);
        // Process matches bottom-to-top so earlier insertions don't shift later positions
        matches.stream()
                .sorted(Comparator.comparingInt(AnnotationMatch::line).reversed()
                        .thenComparing(Comparator.comparingInt(AnnotationMatch::column).reversed()))
                .forEach(m -> {
                    int idx = m.line() - 1;
                    String line = lines[idx];
                    int insertPos = m.column() - 1;
                    lines[idx] = line.substring(0, insertPos) + SEARCH_MARKER + line.substring(insertPos);
                });
        return String.join("\n", lines);
    }

    private static List<AnnotationMatch> collectMatches(CompilationUnit cu, String annotationPattern) {
        List<AnnotationMatch> matches = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MarkerAnnotationExpr n, Void arg) {
                check(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(NormalAnnotationExpr n, Void arg) {
                check(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(SingleMemberAnnotationExpr n, Void arg) {
                check(n);
                super.visit(n, arg);
            }

            private void check(AnnotationExpr annotation) {
                if (matchesPattern(annotation.getNameAsString(), annotationPattern)) {
                    String className = annotation.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse("<unknown>");
                    int line = annotation.getBegin().map(p -> p.line).orElse(-1);
                    int column = annotation.getBegin().map(p -> p.column).orElse(-1);
                    matches.add(new AnnotationMatch(annotation.getNameAsString(), className, line, column));
                }
            }
        }, null);

        return matches;
    }

    private static boolean matchesPattern(String annotationName, String pattern) {
        String clean = pattern.startsWith("@") ? pattern.substring(1) : pattern;
        String simpleName = clean.contains(".")
                ? clean.substring(clean.lastIndexOf('.') + 1)
                : clean;
        return annotationName.equals(simpleName) || annotationName.equals(clean);
    }
}