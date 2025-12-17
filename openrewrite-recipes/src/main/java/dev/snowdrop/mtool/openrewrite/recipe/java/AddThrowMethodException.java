package dev.snowdrop.mtool.openrewrite.recipe.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddThrowMethodException extends Recipe {

	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Option(displayName = "New method exception", description = "The fully qualified name of an exception to add to the method.", example = "java.netUriException")
	String newException;

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

				if (methodMatcher.matches(m.getMethodType())) {

					Space before = m.getPadding().getParameters().getLastSpace();
					if (before.getWhitespace().isEmpty()) {
						m = m.withThrows(List.of(TypeTree.build(newException)));

						// Include a space between the throws word and the method declaration
						m = m.getPadding()
								.withThrows(m.getPadding().getThrows().withBefore(before.withWhitespace(" ")));
					}

					// Add a space between the throws word and the exception
					m = m.withThrows(ListUtils.mapFirst(m.getThrows(), aThrows -> {
						if (aThrows.getPrefix().getWhitespace().isEmpty()) {
							return aThrows.withPrefix(aThrows.getPrefix().withWhitespace(" "));
						}
						return autoFormat(aThrows, ctx);
					}));

					/*m = m.withThrows(List.of(TypeTree.build(newException)));
					maybeAddImport(newException);
					return autoFormat(m, ctx);*/
				}
				return m;
			}
		};
	}
}
