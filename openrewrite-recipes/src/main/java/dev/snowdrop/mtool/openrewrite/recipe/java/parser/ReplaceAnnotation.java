package dev.snowdrop.mtool.openrewrite.recipe.java.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.ArrayList;
import java.util.List;

public class ReplaceAnnotation {

    public record ReplacementResult(String source, int replacementCount) {
    }

    public static ReplacementResult replace(String sourceCode,
            String annotationPatternToReplace,
            String annotationTemplateToInsert) {
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);
        AnnotationExpr replacement = StaticJavaParser.parseAnnotation(annotationTemplateToInsert);

        String cleanPattern = annotationPatternToReplace.startsWith("@")
                ? annotationPatternToReplace.substring(1)
                : annotationPatternToReplace;
        String simplePattern = cleanPattern.contains(".")
                ? cleanPattern.substring(cleanPattern.lastIndexOf('.') + 1)
                : cleanPattern;

        String insertClean = annotationTemplateToInsert.startsWith("@")
                ? annotationTemplateToInsert.substring(1)
                : annotationTemplateToInsert;
        String insertFqn = insertClean.contains("(")
                ? insertClean.substring(0, insertClean.indexOf('('))
                : insertClean;

        if (insertFqn.contains(".")) {
            String insertSimple = insertFqn.substring(insertFqn.lastIndexOf('.') + 1);
            replacement.setName(new Name(insertSimple));
        }

        List<String> oldImports = new ArrayList<>();
        int[] count = { 0 };

        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public Visitable visit(MarkerAnnotationExpr n, Void arg) {
                if (matches(n, simplePattern, cleanPattern)) {
                    collectImportToRemove(n, oldImports);
                    count[0]++;
                    return replacement.clone();
                }
                return super.visit(n, arg);
            }

            @Override
            public Visitable visit(NormalAnnotationExpr n, Void arg) {
                if (matches(n, simplePattern, cleanPattern)) {
                    collectImportToRemove(n, oldImports);
                    count[0]++;
                    return replacement.clone();
                }
                return super.visit(n, arg);
            }

            @Override
            public Visitable visit(SingleMemberAnnotationExpr n, Void arg) {
                if (matches(n, simplePattern, cleanPattern)) {
                    collectImportToRemove(n, oldImports);
                    count[0]++;
                    return replacement.clone();
                }
                return super.visit(n, arg);
            }
        }, null);

        if (count[0] > 0) {
            for (String oldImport : oldImports) {
                cu.getImports().removeIf(imp -> imp.getNameAsString().equals(oldImport));
            }
            if (insertFqn.contains(".")) {
                cu.addImport(new ImportDeclaration(insertFqn, false, false));
            }
        }

        return new ReplacementResult(cu.toString(), count[0]);
    }

    private static boolean matches(AnnotationExpr annotation, String simplePattern, String fullPattern) {
        String name = annotation.getNameAsString();
        return name.equals(simplePattern) || name.equals(fullPattern);
    }

    private static void collectImportToRemove(AnnotationExpr annotation, List<String> imports) {
        annotation.findCompilationUnit().ifPresent(cu -> cu.getImports().stream()
                .filter(imp -> !imp.isAsterisk()
                        && imp.getNameAsString().endsWith("." + annotation.getNameAsString()))
                .findFirst()
                .ifPresent(imp -> imports.add(imp.getNameAsString())));
    }
}