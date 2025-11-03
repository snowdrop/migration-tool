package org.openrewrite.quarkus;

import dev.snowdrop.openrewrite.recipe.spring.CreateJavaClassFromTemplate;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class CreateJavaClassTest implements RewriteTest {

	private final String classTemplate = """
			package %s;

			%sclass %s implements QuarkusApplication {
			    @Override
			    public int run(String... args) throws Exception {
			      System.out.println("Hello Todo user " + args[0]);
			      return 0;
			    }
			}
			""";

	@Test
	void shouldCreateTodoApplicationClass() {
		rewriteRun(
				spec -> spec.recipe(new CreateJavaClassFromTemplate(classTemplate, "src/main/java",
						"org.openrewrite.example", "public", "TodoApplication", null, "foo/bar")),
				java(doesNotExist(), """
						package org.openrewrite.example;

						public class TodoApplication implements QuarkusApplication {
						    @Override
						    public int run(String... args) throws Exception {
						      System.out.println("Hello Todo user " + args[0]);
						      return 0;
						    }
						}
						""", spec -> spec.path("foo/bar/src/main/java/org/openrewrite/example/TodoApplication.java")));
	}

}
