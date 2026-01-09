package dev.snowdrop.mtool.openrewrite.recipe.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.List;

/**
 * OpenRewrite recipe that adds missing import statements to Java classes.
 *
 * <p>This recipe identifies method calls that match a specified pattern and automatically
 * adds the corresponding import statements to the class. This is useful for migration
 * scenarios where new dependencies or packages need to be imported.</p>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddMissingImport extends Recipe {
	@Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
	String methodPattern;

	@Option(displayName = "Missing imports to be added", description = "The list of the missing import to be added")
	String newImports;

	@Override
	public String getDisplayName() {
		return "Add new import to the class matching the method pattern";
	}

	@Override
	public String getDescription() {
		return "Changes the return type of a method invocation.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaIsoVisitor<ExecutionContext>() {
			private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

				if (methodMatcher.matches(m.getMethodType())) {
					List<String> imports = List.of(newImports.split(","));
					imports.forEach(i -> new AddImport(newImports, null, false));
				}
				return m;
			}
		};
	}
}
