package dev.snowdrop.openrewrite.java.search;

import dev.snowdrop.openrewrite.java.table.AnnotationsReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindAnnotations extends FindRecipe {

    @Option(displayName = "Match on meta annotations", description = "When enabled, matches on meta annotations of the annotation pattern.", required = false)
    @Nullable
    Boolean matchMetaAnnotations;

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    public transient AnnotationsReport report = new AnnotationsReport(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(pattern, matchMetaAnnotations);
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {

            Path sourcePath;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourcePath = sourceFile.getSourcePath();
                }
                return super.visit(tree, executionContext);
            }

            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                    if (annotationMatcher.matchesAnnotationOrMetaAnnotation(TypeUtils.asFullyQualified(type))) {
                        J.CompilationUnit aCu = getCursor().getValue();
                        J.ClassDeclaration aClass = aCu.getClasses().getFirst();
                        report.insertRow(ctx,
                                new AnnotationsReport.Row(matchId, AnnotationsReport.Type.JAVA,
                                        AnnotationsReport.Symbol.ANNOTATION, pattern, sourcePath.toString(),
                                        // FQName of the class containing the Annotation
                                        aClass.getName().getSimpleName()));
                        return SearchResult.found(cu);
                    }
                }
                return tree;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            Path sourcePath;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourcePath = sourceFile.getSourcePath();
                }
                return super.visit(tree, executionContext);
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (annotationMatcher.matches(annotation)) {
                    J.ClassDeclaration aClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    System.out.printf("Class name: %s%n", aClass.getName());

                    report.insertRow(ctx,
                            new AnnotationsReport.Row(matchId, AnnotationsReport.Type.JAVA,
                                    AnnotationsReport.Symbol.ANNOTATION, pattern, sourcePath.toString(),
                                    // FQName of the class containing the Annotation
                                    aClass.getName().getSimpleName()));
                    return SearchResult.found(a);
                }
                return a;
            }
        });
    }

    public static Set<J.Annotation> find(J j, String annotationPattern) {
        return find(j, annotationPattern, false);
    }

    public static Set<J.Annotation> find(J j, String annotationPattern, boolean matchMetaAnnotations) {
        return TreeVisitor
                .collect(new org.openrewrite.java.search.FindAnnotations(annotationPattern, matchMetaAnnotations)
                        .getVisitor(), j, new HashSet<>())
                .stream().filter(a -> a instanceof J.Annotation).map(a -> (J.Annotation) a).collect(toSet());
    }
}
