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

/*
   The purpose of this recipe is to:
   - Find the Java Class having as Annotation class: @QuarkusMain
   - Check if the class includes a public void static main() method
   - Use the main method arguments to pass them to the Quarkus.run(); method to be included within the body of the method
   - Add the import package: io.quarkus.runtime.Quarkus

   package com.todo.app;

   import io.quarkus.runtime.annotations.QuarkusMain;
   import io.quarkus.runtime.Quarkus;

   @QuarkusMain
   public class AppApplication {
     public static void main(String[] args) {
            Quarkus.run(args);
     }
   }

 */
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
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, ExecutionContext ctx) {
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
                    J.MethodDeclaration n = getCursor().firstEnclosing(J.MethodDeclaration.class);

                    Parameter p = findParameter(m.getParameters(), 0);
                    System.out.println("Method parameter: " + p);

                    J.VariableDeclarations param = (J.VariableDeclarations) m.getParameters().getFirst();
                    J.VariableDeclarations.NamedVariable variable = param.getVariables().getFirst();

                    System.out.println("Body: " + m.getBody().print());

                    return JavaTemplate
                        .builder("Quarkus.run(#{any(java.lang.String[])});")
                        .javaParser(JavaParser.fromJavaVersion().classpath("quarkus-core"))
                        .imports("io.quarkus.runtime.Quarkus")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replaceBody(), variable.getName());
                }
            }
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
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

    private Parameter findParameter(List<Statement> parameters, int pos) {
        Parameter parameter = null;
        J.VariableDeclarations var = (J.VariableDeclarations) parameters.get(pos);

        // Extract the type and name
        String paramType = var.getType().toString();
        // A parameter declaration usually has only one variable, so we get it from index 0
        String paramName = var.getVariables().get(0).getSimpleName();

        // Create and return the new Parameter record in one step
        return new Parameter(paramName, paramType);
    }

    public record Parameter(
        String name,
        String type
    ) {
    }
}
