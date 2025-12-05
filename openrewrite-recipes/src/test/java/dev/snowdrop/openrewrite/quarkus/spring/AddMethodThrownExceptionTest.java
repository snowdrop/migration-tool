package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.AddThrowMethodException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeMethodInvocationReturnType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AddMethodThrownExceptionTest implements RewriteTest {

	@Test
	@DisplayName("Add an exception ot the method.")
	void addAnExceptionToMethodDeclared() {
		String methodPattern = "TaskController addMessage(..)";
		String newException = "java.lang.Exception";

		rewriteRun(spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
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

					// Add now a space between the throws word and the exception
					m = m.withThrows(ListUtils.mapFirst(m.getThrows(), aThrows -> {
						if (aThrows.getPrefix().getWhitespace().isEmpty()) {
							return aThrows.withPrefix(aThrows.getPrefix().withWhitespace(" "));
						}
						return aThrows;
					}));

					maybeAddImport(newException);
				}

				return m;
			}
		})).expectedCyclesThatMakeChanges(1).cycles(1), java("""
				public class TaskController {
				  String addMessage(String msg) {
				      return "hi";
				  }
				}
				""", """
				  public class TaskController {
				    String addMessage(String msg) throws java.lang.Exception {
				        return "hi";
				    }
				  }
				"""));
	}
}
