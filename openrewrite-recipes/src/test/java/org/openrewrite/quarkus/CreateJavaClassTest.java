package org.openrewrite.quarkus;

import dev.snowdrop.openrewrite.recipe.spring.CreateJavaClassFromTemplate;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class CreateJavaClassTest implements RewriteTest {

    private String classTemplate = """
      package %s;
      
      %sclass %s {
      }
      """;

    @Test
    void shouldCreateTodoApplicationClass() {
        rewriteRun(
            spec -> spec.recipe(new CreateJavaClassFromTemplate(
                classTemplate,
                "src/main/java",
                "org.openrewrite.example",
                "package-private",
                "TodoApplication",
                null,
                "foo/bar"
            )),
            java(
                doesNotExist(),
                """
                  package org.openrewrite.example;
    
                  class TodoApplication {
                  }
                  """,
                spec -> spec.path("foo/bar/src/main/java/org/openrewrite/example/TodoApplication.java")
            )
        );
    }

}
