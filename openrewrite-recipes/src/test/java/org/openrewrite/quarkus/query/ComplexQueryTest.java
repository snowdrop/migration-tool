package org.openrewrite.quarkus.query;

import dev.snowdrop.openrewrite.recipe.query.QueryRecipe;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ComplexQueryTest implements RewriteTest {

    String pomXML = """
        <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.myorg</groupId>
          <artifactId>my-app</artifactId>
          <version>1.0.0</version>
          <dependencies>
            <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-core</artifactId>
              <version>3.16.2</version>
            </dependency>
            <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-rest</artifactId>
              <version>3.16.2</version>
            </dependency>
          </dependencies>
        </project>
        """;

    @Disabled
    @Test
    void matchTwoDependenciesUsingAndOperator() {
        rewriteRun(
            spec -> spec.recipe(new QueryRecipe(
                "FIND DEPENDENCY IN POM WHERE (artifactId='quarkus-core', version='3.16.2') AND (artifactId='quarkus-rest' AND version='3.16.2')"
            )),
            pomXml(
                pomXML,
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.myorg</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-core</artifactId>
                      <version>3.16.2</version>
                    </dependency>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-rest</artifactId>
                      <version>3.16.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
        );
    }

    @Test
    void findMultipleDependenciesUsingOrOperator() {
        rewriteRun(
            spec -> spec.recipe(new QueryRecipe(
                "FIND DEPENDENCY IN POM WHERE (artifactId='quarkus-core', version='3.16.2') OR (artifactId='quarkus-rest' AND version='3.16.2')"
            )),
            pomXml(
                pomXML,
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.myorg</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <!--~~(found dependency by query)~~>--><dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-core</artifactId>
                      <version>3.16.2</version>
                    </dependency>
                    <!--~~(found dependency by query)~~>--><dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-rest</artifactId>
                      <version>3.16.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
        );
    }
}