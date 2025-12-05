package dev.snowdrop.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.ChangeMethodReturnType;
import dev.snowdrop.openrewrite.recipe.spring.RemoveMethodParameters;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceMethodBodyContent;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;

public class CleanParamsReplaceBodyChangeMethodReturnTypeTest implements RewriteTest {

	@Test
	void replaceMethodSignature() {
		rewriteRun(spec -> spec
				.recipe(new CompositeRecipe(List.of(new RemoveMethodParameters("TaskController viewHome(..)"),
						new ReplaceMethodBodyContent("TaskController addMessage(..)",
								"return new StringBuilder().append(msg).toString();"),
						new ChangeMethodReturnType("TaskController addMessage(..)", "Object"))))
				.expectedCyclesThatMakeChanges(1).cycles(1), java("""
						public class TaskController {
						  public String viewHome(String msg) {
						    String res = addMessage("hi");
						  }
						  String addMessage(String msg) {
						    return msg;
						  }
						}
						""", """
						  public class TaskController {
						    public String viewHome() {
						      String res = addMessage("hi");
						    }
						     Object addMessage(String msg) {
						        return new StringBuilder().append(msg).toString();
						    }
						  }
						"""));
	}
}
