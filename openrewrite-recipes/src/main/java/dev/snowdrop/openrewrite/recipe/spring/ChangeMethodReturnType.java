package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodReturnType extends Recipe {

	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Option(displayName = "New method invocation return type", description = "The fully qualified new return type of method invocation.", example = "long")
	String newReturnType;

	@Override
	public String getDisplayName() {
		return "Change method invocation return type";
	}

	@Override
	public String getDescription() {
		return "Changes the return type of a method invocation.";
	}

	@Override
	public Validated<Object> validate() {
		return super.validate().and(MethodMatcher.validate(methodPattern));
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {

		return new JavaIsoVisitor<ExecutionContext>() {
			private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
				//JavaType.Method type = m.getMethodType();

				if (methodMatcher.matches(m.getMethodType())) {
					System.out.println("========== BEFORE ==========");
					System.out.printf("Method name: %s \n", m.getSimpleName());
					System.out.printf("Method modifiers: %s \n", m.getModifiers());
					System.out.printf("Return Type: %s \n", m.getType());
                    System.out.printf("Return Type expression: %s \n", m.getReturnTypeExpression());
;
					//type = type.withReturnType(JavaType.buildType(newReturnType));
					//m = m.withMethodType(type);
                    m = m.withReturnTypeExpression(TypeTree.build(" " + newReturnType));
                    m = m.withMethodType(m.getMethodType().withReturnType(JavaType.buildType(newReturnType)));

					System.out.println("========== AFTER ==========");
					System.out.printf("Method name: %s \n", m.getSimpleName());
					System.out.printf("Method modifiers: %s \n", m.getModifiers());
					System.out.printf("Return Type: %s \n", m.getType());
                    System.out.printf("Return Type expression: %s \n", m.getReturnTypeExpression());
				}
				return m;
			}

			/*
			@Override
			public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
			    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
			    JavaType.Method type = m.getMethodType();

			    if (methodMatcher.matches(m.getMethodType())) {
			        System.out.printf("Method name: %s \n",m.getSimpleName());
			        System.out.printf("Method type arguments: %s \n",m.getArguments());
			        System.out.printf("Return Type: %s \n",m.getType());
			        System.out.printf("Flags: %s \n",m.getMethodType().getFlags());
			        System.out.printf("Declaring type: %s \n",m.getMethodType().getDeclaringType());

			        return m.withMethodType(type.withReturnType(JavaType.buildType(newReturnType)));
			    }

			    return m;
			}

			 */
		};
	}
}
