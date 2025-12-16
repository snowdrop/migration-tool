package dev.snowdrop.mtool.openrewrite.recipe.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveMethodParameters extends Recipe {

	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Override
	public @NlsRewrite.DisplayName String getDisplayName() {
		return "Replace method body content";
	}

	@Override
	public @NlsRewrite.Description String getDescription() {
		return "Replace method body content.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {

		return new JavaIsoVisitor<ExecutionContext>() {
			private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {

				if (methodMatcher.matches(method.getMethodType())) {
					if (!method.getParameters().isEmpty()) {
						// Remove the parameters of the method.
						// Example foo(String bar) => foo()
						return method.withParameters(Collections.emptyList());
					}
				}
				return super.visitMethodDeclaration(method, ctx);
			}
		};
	}
}