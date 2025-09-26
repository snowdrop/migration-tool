package dev.snowdrop.openrewrite.recipe.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

public class ReplaceSpringBootApplicationAnnotationWithQuarkusMainAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert the @SpringBootApplication annotation to @QuarkusMain";
    }

    @Override
    public String getDescription() {
        return "Convert the @SpringBootApplication annotation to @QuarkusMain.";
    }

    transient SpringBootScanReport report = new SpringBootScanReport(this);

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

            if ("SpringBootApplication".equals(simpleName)) {
                report.insertRow(ctx, new SpringBootScanReport.Row(simpleName, "n/a"));

                maybeRemoveImport("org.springframework.boot.autoconfigure.SpringBootApplication");
                maybeRemoveImport("org.springframework.boot.SpringApplication");
                maybeAddImport("io.quarkus.runtime.annotations.QuarkusMain");

                // Replace with QuarkusMain annotation
                return JavaTemplate.builder("@QuarkusMain")
                    .javaParser(JavaParser.fromJavaVersion().classpath("quarkus-core"))
                    .imports("io.quarkus.runtime.annotations.QuarkusMain")
                    .build()
                    .apply(getCursor(), a.getCoordinates().replace());
            }
            return a;
        }
    }
}
