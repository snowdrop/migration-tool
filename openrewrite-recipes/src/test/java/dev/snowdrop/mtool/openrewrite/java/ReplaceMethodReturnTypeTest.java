package dev.snowdrop.mtool.openrewrite.java;

import dev.snowdrop.mtool.openrewrite.recipe.java.ChangeMethodReturnType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeMethodInvocationReturnType;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceMethodReturnTypeTest implements RewriteTest {

	@Test
	@DisplayName("Replace the type of the variable calling a method.")
	void replaceReturnTypeOfMethodInvoked() {
		rewriteRun(spec -> spec.recipe(new ChangeMethodInvocationReturnType("TaskController addMessage(..)", "Object"))
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
		String newReturnType = "java.util.List<String>";

		rewriteRun(spec -> spec.recipe(new ChangeMethodReturnType(methodPattern, newReturnType))
				.expectedCyclesThatMakeChanges(1).cycles(1), java("""
						public class TaskController {
						  String addMessage(String msg) {
						      return null;
						  }
						}
						""", """
						  import java.util.List;

						  public class TaskController {
						      List<String> addMessage(String msg) {
						          return null;
						      }
						  }
						"""));
	}
}
