package dev.snowdrop.openrewrite.quarkus.spring;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.ChangeMethodInvocationReturnType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class ReplaceMethodReturnTypeTest implements RewriteTest {

	@Test
	@DisplayName("Replace the type of the variable calling a method.")
	void replaceReturnTypeOfMethodInvoked() {
		rewriteRun(spec -> spec
				.recipe(new CompositeRecipe(
						List.of(new ChangeMethodInvocationReturnType("TaskController addMessage(..)", "Object"))))
				.expectedCyclesThatMakeChanges(1).cycles(1), java("""
						public class TaskController {
						  public String viewHome(String msg) {
						    String res = addMessage(msg);
						  }
						  String addMessage(String msg) {
						      return new StringBuilder().append("Hi").append(msg).toString();
						  }
						}
						""", """
						  public class TaskController {
						    public String viewHome(String msg) {
						      Object res = addMessage(msg);
						    }
						    String addMessage(String msg) {
						        return new StringBuilder().append("Hi").append(msg).toString();
						    }
						  }
						"""));
	}

	@Test
	@Disabled // This test is disabled due to this error: https://github.com/openrewrite/rewrite/issues/6379
	@DisplayName("Replace the return type of a method.")
	void replaceReturnTypeOfMethodDeclared() {
		String methodPattern = "TaskController addMessage(..)";
		String newReturnType = "Object";

		rewriteRun(spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
			final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);
			static boolean methodUpdated = false;

			@Override
			public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

				if (methodMatcher.matches(m.getMethodType())) {
					System.out.println("========== BEFORE ==========");
					System.out.printf("Method name: %s \n", m.getSimpleName());
					System.out.printf("Method modifiers: %s \n", m.getModifiers());
					System.out.printf("Return Type: %s \n", m.getType());
					System.out.printf("Declaring type: %s \n", m.getMethodType().getDeclaringType());

					JavaType.Method newType = m.getMethodType().withReturnType(JavaType.buildType(newReturnType));
					m = m.withMethodType(newType).withName(m.getName().withType(newType));

					System.out.println("========== AFTER ==========");
					System.out.printf("Method name: %s \n", m.getSimpleName());
					System.out.printf("Method modifiers: %s \n", m.getModifiers());
					System.out.printf("New return Type: %s \n", m.getMethodType().getReturnType());
					System.out.printf("Declaring type: %s \n", m.getMethodType().getDeclaringType());
					methodUpdated = true;
				}
				return m;
			}
		})).expectedCyclesThatMakeChanges(1).cycles(1), java("""
				public class TaskController {
				  public String viewHome(String msg) {
				    var res = addMessage(msg);
				  }
				  protected String addMessage(String msg) {
				      return new StringBuilder().append("Hi").append(msg).toString();
				  }
				}
				""", """
				  public class TaskController {
				    public String viewHome(String msg) {
				      var res = addMessage(msg);
				    }
				    protected Object addMessage(String msg) {
				        return new StringBuilder().append("Hi").append(msg).toString();
				    }
				  }
				"""));
	}
}
