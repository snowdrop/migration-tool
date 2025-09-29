package dev.snowdrop.openrewrite.recipe.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class AddQuarkusRun extends Recipe {

    private static final MethodMatcher QUARKUS_MAIN_RUN_MATCHER = new MethodMatcher("io.quarkus.runtime.Quarkus run()");

    @Override
    public String getDisplayName() {
        return "Replace SpringApplication.run() method with Quarkus.run()";
    }

    @Override
    public String getDescription() {
        return "Replace SpringApplication.run() method with Quarkus.run()";
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
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (QUARKUS_MAIN_RUN_MATCHER.matches(method)) {
                if (! m.getArguments().isEmpty() && m.getArguments().size() > 1) {
                    maybeAddImport("io.quarkus.runtime.Quarkus");
                    return JavaTemplate.builder("Quarkus.run(#{any(java.lang.String[])})")
                        .javaParser(JavaParser.fromJavaVersion().classpath("quarkus-core"))
                        .imports("io.quarkus.runtime.Quarkus")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(1));
                }
            }
            return m;
        }
    }
}
