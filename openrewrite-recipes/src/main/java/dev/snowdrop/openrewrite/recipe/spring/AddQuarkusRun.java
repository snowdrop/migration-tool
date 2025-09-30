package dev.snowdrop.openrewrite.recipe.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class AddQuarkusRun extends Recipe {

    private static final String QUARKUS_MAIN_ANNOTATION = "@io.quarkus.runtime.annotations.QuarkusMain";

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

    public AnnotationMatcher quarkusMainAnnotationMatcher = new AnnotationMatcher(QUARKUS_MAIN_ANNOTATION);

    private class SpringBootToQuarkusMainVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(methodDeclaration, ctx);

            JavaType.Method mType = m.getMethodType();

            System.out.println("Visit Method declaration processed for: ");
            System.out.println("Name: " + m.getSimpleName());
            System.out.println("Method type: " + m.getMethodType().getName());
            System.out.println("Return type: " + mType.getReturnType());

            boolean hasStaticModifier = J.Modifier.hasModifier(m.getModifiers(), J.Modifier.Type.Static);
            System.out.println("Has static modifier: " + hasStaticModifier);

            if ("main".equals(m.getSimpleName()) &&
                "void".equals(mType.getReturnType().toString()) &&
                hasStaticModifier) {

                J.ClassDeclaration parentClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                System.out.println("Processing the main method of the class: " + parentClass.getSimpleName());

                if (hasAnnotation(parentClass.getLeadingAnnotations(), "QuarkusMain")) {
                    System.out.println("Processing the Java Class including the static main method and having as Class annotation: @QuarkusMain");

                    return JavaTemplate
                        .builder("Quarkus.run();") // java.lang.String[]
                        .contextSensitive()
                        .build()
                        .apply(getCursor(),m.getCoordinates().replaceBody());
                }
            }
            return m;
        }
    }

    private boolean hasAnnotation(List<J.Annotation> annotations, String annotationName) {
        for (J.Annotation a : annotations) {
            System.out.println("Class annotation name: " + a.getSimpleName());
            if (quarkusMainAnnotationMatcher.matches(a)) {
                return true;
            }
        }
        return false;
    }
}
