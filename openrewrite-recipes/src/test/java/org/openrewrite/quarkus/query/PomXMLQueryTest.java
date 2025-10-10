package org.openrewrite.quarkus.query;

import dev.snowdrop.openrewrite.recipe.query.QueryRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class PomXMLQueryTest implements RewriteTest {

    String pomXML = """
        <project>
          <groupId>com.myorg</groupId>
          <artifactId>my-app</artifactId>
          <version>1.0.0</version>
          <dependencies>
            <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-core</artifactId>
              <version>3.4.3</version>
            </dependency>
            <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-resteasy</artifactId>
              <version>3.4.3</version>
            </dependency>
          </dependencies>
        </project>
        """;

    @Test
    void findQuarkusCoreDependency() {
        rewriteRun(
            // 1. Configure the recipe using a YAML string
            spec -> spec.recipe(new QueryRecipe(
                "FIND DEPENDENCY IN POM WHERE artifactId='quarkus-core'"
            )),
            pomXml(
                pomXML,
                """
                    <project>
                      <groupId>com.myorg</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                        <!--~~(found dependency by query)~~>--><dependency>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-core</artifactId>
                          <version>3.4.3</version>
                        </dependency>
                        <dependency>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-resteasy</artifactId>
                          <version>3.4.3</version>
                        </dependency>
                      </dependencies>
                    </project>
                    """
            )
        );
    }
}