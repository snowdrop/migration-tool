package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.RemoveMethodParameters;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceMethodBodyContent;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.ChangeMethodInvocationReturnType;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

public class ChangeMethodContentTest implements RewriteTest {

	String replacement = """
			var msg = "Hi from me";
			return msg;
			""";

	@Test
	void replaceMethodBodyContent() {
		rewriteRun(spec -> spec.recipe(new ReplaceMethodBodyContent("generateMessage", replacement))
			.expectedCyclesThatMakeChanges(1).cycles(1),
            java("""
				public class TaskController {
				  public String viewHome() {
				    return generateMessage();
				  }
				  String generateMessage() {
				    return "Hi";
				  }
				}
				""",
                """
				public class TaskController {
				  public String viewHome() {
				    return generateMessage();
				  }
				  String generateMessage() {
				      var msg = "Hi from me";
				      return msg;
				  }
				}
				"""));
	}

	@Test
	void replaceMethodSignature() {
		rewriteRun(spec -> spec.recipe(new CompositeRecipe(List.of(
                new RemoveMethodParameters("viewHome()"),
                new ReplaceMethodBodyContent("addAttribute()", "return new StringBuilder().append(\"Hi. This is me !);"),
                new ChangeMethodInvocationReturnType("TaskController addAttribute(..)","java.lang.StringBuilder")
            )))
			.expectedCyclesThatMakeChanges(1).cycles(1),
            java("""
						public class TaskController {
						  public String viewHome(String msg) {
						    return addAttribute("task", new Object());
						  }
						  String addAttribute(String name, Object value) {
						    return "Hi";
						  }
						}
						""", """
						  public class TaskController {
						    public String viewHome() {
						      return addAttribute("task", new Object());
						    }
						    StringBuilder addAttribute(String name, Object value) {
						        return new StringBuilder().append("Hi. This is me !);
						    }
						  }
						"""));
	}
}
