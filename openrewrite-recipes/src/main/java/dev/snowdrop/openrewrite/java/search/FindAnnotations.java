package dev.snowdrop.openrewrite.java.search;

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
import org.openrewrite.quark.Quark;
import org.openrewrite.table.SourcesFiles;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindAnnotations extends Recipe {
    /**
     * An annotation pattern, expressed as a method pattern.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Annotation pattern",
        description = "An annotation pattern, expressed as a method pattern.",
        example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    @Option(displayName = "Match on meta annotations",
        description = "When enabled, matches on meta annotations of the annotation pattern.",
        required = false)
    @Nullable
    Boolean matchMetaAnnotations;

    @Option(displayName = "Match id",
        description = "ID of the matching tool needed to reconcile the records where a match took place",
        required = true)
    String matchId;

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    public transient MatchingReport report = new MatchingReport(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern, matchMetaAnnotations);
        return Preconditions.check(
            new JavaIsoVisitor<ExecutionContext>() {

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
                            report.insertRow(ctx, new MatchingReport.Row(
                                MatchingReport.Type.JAVA,
                                sourcePath.toString(),
                                // FQName of the class containing the Annotation
                                aCu.toString(),
                                annotationPattern,
                                matchId
                            ));
                            return SearchResult.found(cu);
                        }
                    }
                    return tree;
                }
            },
            new JavaIsoVisitor<ExecutionContext>() {
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
                        J.ClassDeclaration aClass = getCursor().getParent().getValue();
                        report.insertRow(ctx, new MatchingReport.Row(
                            MatchingReport.Type.JAVA,
                            sourcePath.toString(),
                            // FQName of the class containing the Annotation
                            aClass.getType().getFullyQualifiedName(),
                            // Name of the annotation (! this is not the FQN)
                            a.getAnnotationType().toString(),
                            matchId
                        ));
                        return SearchResult.found(a);
                    }
                    return a;
                }
            }
        );
    }

    public static Set<J.Annotation> find(J j, String annotationPattern) {
        return find(j, annotationPattern, false);
    }

    public static Set<J.Annotation> find(J j, String annotationPattern, boolean matchMetaAnnotations) {
        return TreeVisitor.collect(
                new org.openrewrite.java.search.FindAnnotations(annotationPattern, matchMetaAnnotations).getVisitor(),
                j,
                new HashSet<>()
            )
            .stream()
            .filter(a -> a instanceof J.Annotation)
            .map(a -> (J.Annotation) a)
            .collect(toSet());
    }
}
