package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveMethodParameters extends Recipe {
	@Override
	public @NlsRewrite.DisplayName String getDisplayName() {
		return "Replace method body content";
	}

	@Override
	public @NlsRewrite.Description String getDescription() {
		return "Replace method body content.";
	}

	@Option(displayName = "Name of the method to search", description = "Name of the method where we will remove the parameters")
	String methodToSearch;

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new MethodBodyMainVisitor();
	}

	private class MethodBodyMainVisitor extends JavaIsoVisitor<ExecutionContext> {

		@Override
		public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
			if (methodToSearch.contains(method.getSimpleName())) {
				if (!method.getParameters().isEmpty()) {
					// Remove the parameters of the method.
					// Example foo(String bar) => foo()
					return method.withParameters(Collections.emptyList());
				}
			}
			return super.visitMethodDeclaration(method, ctx);
		}
	}
}
