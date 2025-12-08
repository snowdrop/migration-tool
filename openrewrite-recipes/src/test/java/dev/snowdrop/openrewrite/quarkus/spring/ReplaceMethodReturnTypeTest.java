package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.ChangeMethodReturnType;
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
import org.openrewrite.java.tree.TypeTree;
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
	@DisplayName("Replace the return type of a method.")
	void replaceReturnTypeOfMethodDeclared() {
		String methodPattern = "TaskController addMessage(..)";
		String newReturnType = "Object";

		rewriteRun(spec -> spec.recipe(new ChangeMethodReturnType(methodPattern, newReturnType))
				.expectedCyclesThatMakeChanges(1).cycles(1), java("""
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
