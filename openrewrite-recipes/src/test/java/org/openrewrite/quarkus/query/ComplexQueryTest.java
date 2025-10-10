package org.openrewrite.quarkus.query;

import dev.snowdrop.openrewrite.recipe.query.QueryRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ComplexQueryTest implements RewriteTest {
    @Test
    void findMultipleDependenciesWithGroupedOr() {
        rewriteRun(
            spec -> spec.recipe(new QueryRecipe(
                "FIND DEPENDENCY IN POM WHERE (artifactId='quarkus-core' AND version='3.16.2') OR (artifactId='quarkus-rest' AND version='3.16.2')"
            )),
            pomXml(
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
                      <artifactId>quarkus-core</artifactId>
                      <version>3.26.4</version>
                    </dependency>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-rest</artifactId>
                      <version>3.16.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
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
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-core</artifactId>
                      <version>3.26.4</version>
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