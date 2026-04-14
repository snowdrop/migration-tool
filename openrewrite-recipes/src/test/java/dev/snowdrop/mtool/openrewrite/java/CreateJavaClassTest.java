package dev.snowdrop.mtool.openrewrite.java;

import dev.snowdrop.mtool.openrewrite.recipe.java.CreateJavaClassFromTemplate;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class CreateJavaClassTest implements RewriteTest {

    private final String classTemplate = """
            package %s;
            import io.quarkus.runtime.QuarkusApplication;
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
        CreateJavaClassFromTemplate javaTodoClass = new CreateJavaClassFromTemplate(classTemplate, "src/main/java",
                "org.openrewrite.example", "public", "TodoApplication", null, "foo/bar", "quarkus-core");
        rewriteRun(
                spec -> spec.recipe(javaTodoClass),
                java(doesNotExist(), """
                        package org.openrewrite.example;
                        import io.quarkus.runtime.QuarkusApplication;
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
