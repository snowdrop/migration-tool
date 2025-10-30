package org.openrewrite.quarkus.spring;

import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class MigrateSpringBootToQuarkusYAMLRecipesTest implements RewriteTest {

    /*
     * Replace the SpringBoot stuffs with Quarkus using a rewrite yaml file having as definition: // TODO: To be removed
     * as test was not doing changes !!! #preconditions: # - org.openrewrite.maven.search.ParentPomInsight: #
     * groupIdPattern: org.springframework.boot # artifactIdPattern: spring-boot-starter-parent # version: 3.x
     * recipeList: - dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation -
     * org.openrewrite.java.RemoveMethodInvocations: methodPattern: "org.springframework.boot.SpringApplication run(..)"
     * - dev.snowdrop.openrewrite.recipe.spring.AddQuarkusRun
     */

    @Test
    void shouldReplaceClassAnnotationUsingYamlRecipe() {
        rewriteRun(s -> s
                .recipeFromResource("/META-INF/rewrite/spring-boot-to-quarkus.yml",
                        "dev.snowdrop.openrewrite.recipe.spring.SpringBootToQuarkus")
                .parser((Parser.Builder) JavaParser.fromJavaVersion()
                        .classpath("spring-context", "spring-boot", "spring-boot-autoconfigure")
                        .logCompilationWarningsAndErrors(true))
                .cycles(1).expectedCyclesThatMakeChanges(1), java(
                        // Before
                        """
                                package com.todo.app;

                                import org.springframework.boot.SpringApplication;
                                import org.springframework.boot.autoconfigure.SpringBootApplication;

                                @SpringBootApplication
                                public class AppApplication {
                                 	public static void main(String[] args) {
                                         SpringApplication.run(AppApplication.class, args);
                                    }
                                }
                                """,
                        // After
                        """
                                package com.todo.app;

                                import io.quarkus.runtime.Quarkus;
                                import io.quarkus.runtime.annotations.QuarkusMain;

                                @QuarkusMain
                                public class AppApplication {
                                 	public static void main(String[] args) {
                                         Quarkus.run(args);
                                     }
                                }
                                """));
    }
}
