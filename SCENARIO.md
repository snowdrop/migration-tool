## Scenario

## From @SpringBootApplication to @QuarkusMain and QuarkusRun

### Introduction

We want to:
1. - Replace the `@SpringBootApplication` annotation with `@QuarkusMain`
2. - Remove the statement `SpringApplication.run(AppApplication.class, args);`
3. - Find the Java Class having as Annotation class: `@QuarkusMain`
   - Check if the class includes a `public void static main()` method
   - Use the main method arguments (String[] args) to pass them to the `Quarkus.run();`
   - Add the import package: `io.quarkus.runtime.Quarkus`

### Code to be changed

Before
```java
package com.todo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 1
public class AppApplication {
    public static void main(String[] args) {
         SpringApplication.run(AppApplication.class, args); // 2
    }
}
```

After
```java
package com.todo.app;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain // 1
public class AppApplication {
    public static void main(String[] args) {
         Quarkus.run(args); // 3
     }
}
```

### Rule definition

```yaml
- category: mandatory
  customVariables: []
  description: Replace the Spring Boot Application Annotation with QuarkusMain
  effort: 1
  labels:
  - konveyor.io/source=springboot
  - konveyor.io/target=quarkus
  links: []
  message: "Replace the Spring Boot Application Annotation with QuarkusMain."
  ruleID: springboot-annotations-to-quarkus-00000
  when:
    java.referenced:
      location: ANNOTATION
      pattern: org.springframework.boot.autoconfigure.SpringBootApplication
  instructions:
    ai:
      - promptMessage: "Remove the org.springframework.boot.autoconfigure.SpringBootApplication annotation from the main Spring Boot Application class"
    manual:
      - todo: "Remove the org.springframework.boot.autoconfigure.SpringBootApplication annotation from the main Spring Boot Application class"
    openrewrite:
      - name: Migrate Spring Boot to Quarkus
        description: Migrate Spring Boot to Quarkus
        preconditions:
          - name: org.openrewrite.java.dependencies.search.ModuleHasDependency
            groupIdPattern: org.springframework.boot
            artifactIdPattern: spring-boot
            version: '[3.5,)'
        recipeList:
          - dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation
          - org.openrewrite.java.RemoveMethodInvocations:
              methodPattern: "org.springframework.boot.SpringApplication run(..)"
          - dev.snowdrop.openrewrite.recipe.spring.AddQuarkusRun
        gav:
          - dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT
```
