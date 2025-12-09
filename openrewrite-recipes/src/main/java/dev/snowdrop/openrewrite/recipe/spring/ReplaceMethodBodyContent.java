package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMethodBodyContent extends Recipe {

	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Option(displayName = "String replaced", description = "String replaced")
	String replacement;

	@Option(displayName = "New imports to be added", description = "The list of the new import to be added.", example = "long", required = false)
	String newImports;

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

					JavaTemplate t;
					if (newImports != null && !newImports.isEmpty()) {
						String[] IMPORTS = newImports.split(",\\s*");
						List.of(IMPORTS).forEach(i -> maybeAddImport(i));
						t = JavaTemplate.builder(replacement).imports(IMPORTS).build();
					} else {
						t = JavaTemplate.builder(replacement).build();
					}
					return autoFormat(t.apply(getCursor(), method.getCoordinates().replaceBody()), ctx);
				}
				return super.visitMethodDeclaration(method, ctx);
			}
		};
	}
}