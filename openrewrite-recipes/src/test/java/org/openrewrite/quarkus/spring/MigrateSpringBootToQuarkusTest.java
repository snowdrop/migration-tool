package org.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.AddQuarkusRun;
import dev.snowdrop.openrewrite.recipe.spring.CreateJavaClassFromTemplate;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RemoveMethodInvocations;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.openrewrite.java.Assertions.java;

public class MigrateSpringBootToQuarkusTest implements RewriteTest {

    @Test
    void ShouldReplaceAnnotationAndMain() {

        rewriteRun(spec -> spec.recipes(
                new ReplaceSpringBootApplicationWithQuarkusMainAnnotation(),
                new RemoveMethodInvocations("org.springframework.boot.SpringApplication run(..)"),
                new AddQuarkusRun(null)
            )
            .parser(JavaParser.fromJavaVersion().classpath("spring-boot"))
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
            java(
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
                    """
            )
        );
    }

    @Test
    @Disabled
    void ShouldReplaceAndHaveTodoApplication() {
            String todoClassTemplate = """
            package %s;
            %sclass %s implements QuarkusApplication {
                @Override
                public int run(String... args) throws Exception {
                  System.out.println("Hello user " + args[0]);
                  return 0;
                }
            }
            """;

        rewriteRun(spec -> spec.recipes(
                    new ReplaceSpringBootApplicationWithQuarkusMainAnnotation(),
                    new RemoveMethodInvocations("org.springframework.boot.SpringApplication run(..)"),
                    new CreateJavaClassFromTemplate(todoClassTemplate,"src/main/java","com.todo.app","public","TodoApplication", false,""),
                    new AddQuarkusRun("")
                )

                .parser((Parser.Builder) JavaParser.fromJavaVersion().dependsOn("""
                package com.todo.app;
                public class TodoApplication implements QuarkusApplication {
                        @Override
                        public int run(String... args) throws Exception {
                          System.out.println("Hello user " + args[0]);
                          return 0;
                        }
                }
                """).classpath("spring-boot","quarkus-core"))
                //.afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion().classpath("spring-boot"))
                .cycles(1)
                .expectedCyclesThatMakeChanges(1),
            java(
                doesNotExist(),
                """
                package com.todo.app;
                public class TodoApplication implements QuarkusApplication {
                    @Override
                    public int run(String... args) throws Exception {
                      System.out.println("Hello user " + args[0]);
                      return 0;
                    }
                }
                """
            ),
            java(
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
                    """
            )
        );
    }
}
