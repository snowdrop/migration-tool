package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.ReplaceMethodBodyContent;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

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
						  String generateMessage() {
						    return "Hi";
						  }
						}
						""", """
						public class TaskController {
						  String generateMessage() {
						      var msg = "Hi from me";
						      return msg;
						  }
						}
						"""));
	}
}
