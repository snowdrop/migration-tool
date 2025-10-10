package org.openrewrite.quarkus.query;

import dev.snowdrop.openrewrite.recipe.query.QueryRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.java.Assertions.java;

public class JavaQueryTest implements RewriteTest {

    String JAVA_CLASS_BEFORE = """
                  package org.openrewrite.example;
                  
                  public class TodoApplication {
                      public int run(String... args) throws Exception {
                        System.out.println("Hello Todo user " + args[0]);
                        return 0;
                      }
                  }
                  """;

    @Test
    public void testJavaQuery() {
        rewriteRun(spec -> spec.recipe(new QueryRecipe(
            "FIND CLASS IN JAVA WHERE name='TodoApplication'"
            )),
            java(
                JAVA_CLASS_BEFORE,
                """
                  package org.openrewrite.example;
                  
                  /*~~(found class by query)~~>*/public class TodoApplication {
                      public int run(String... args) throws Exception {
                        System.out.println("Hello Todo user " + args[0]);
                        return 0;
                      }
                  }
                  """
            )
        );
    }
}
