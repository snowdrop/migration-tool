package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMethodBodyContent extends Recipe {

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

	@Option(displayName = "String replaced", description = "String replaced")
	String replacement;

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaIsoVisitor<ExecutionContext>() {
			private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				JavaTemplate t = JavaTemplate.builder(replacement).build();
				if (methodMatcher.matches(method.getMethodType())) {
					return t.apply(getCursor(), method.getCoordinates().replaceBody());
				}
				return super.visitMethodDeclaration(method, ctx);
			}
		};
	}
}