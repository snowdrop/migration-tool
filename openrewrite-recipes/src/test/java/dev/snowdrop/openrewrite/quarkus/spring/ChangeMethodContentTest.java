package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.ChangeMethodReturnType;
import dev.snowdrop.openrewrite.recipe.spring.RemoveMethodParameters;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceMethodBodyContent;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
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
		rewriteRun(spec -> spec.recipe(new ReplaceMethodBodyContent("TaskController generateMessage()", replacement))
				.expectedCyclesThatMakeChanges(1).cycles(1), java("""
						public class TaskController {
						  public String viewHome() {
						    return generateMessage();
						  }
						  String generateMessage() {
						    return "Hi";
						  }
						}
						""", """
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
}
