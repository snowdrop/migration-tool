package dev.snowdrop.openrewrite.recipe.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class ReplaceSpringBootApplicationWithQuarkusMainAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert the @SpringBootApplication annotation to @QuarkusMain";
    }

    @Override
    public String getDescription() {
        return "Convert the @SpringBootApplication annotation to @QuarkusMain.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SpringBootToQuarkusMainVisitor();
    }

    private class SpringBootToQuarkusMainVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            String simpleName = a.getSimpleName();

            AnnotationMatcher matcher = new AnnotationMatcher("@org.springframework.boot.autoconfigure.SpringBootApplication");

            if (!matcher.matches(a)) {
                return a;
            }


            maybeRemoveImport(TypeUtils.asFullyQualified(a.getType()));
            maybeAddImport("io.quarkus.runtime.QuarkusMain");

            // Replace with QuarkusMain annotation
            return JavaTemplate.builder("@QuarkusMain")
                .javaParser(JavaParser.fromJavaVersion().classpath("quarkus-core"))
                .imports("io.quarkus.runtime.annotations.QuarkusMain")
                .build()
                .apply(getCursor(), a.getCoordinates().replace());

        }
    }
}
