## Scenario

## From @SpringBootApplication to @QuarkusMain and QuarkusRun

### Introduction

We want to:
1. - Replace the `@SpringBootApplication` annotation with `@QuarkusMain`
2. - Remove the statement `SpringApplication.run(AppApplication.class, args);`
3. - Find the Java Class having as Annotation class: `@QuarkusMain`
   - Check if the class includes a `public void static main()` method
   - Find the `method` parameters (String[] args) to pass them to the `Quarkus.run();`
   - Add the import package: `io.quarkus.runtime.Quarkus`
4. - Add a Java class implementing QuarkusApplication

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
and `TodoApplication`
```java
package com.todo.app;

//4
public class TodoApplication implements QuarkusApplication { 
    @Override
    public int run(String... args) throws Exception {
      System.out.println("Hello user " + args[0]);
      return 0;
    }
}
```

### Enhanced Rule definition with instructions

**Steps: 1, 2 and 3**
```yaml
- category: mandatory
  customVariables: []
  description: SpringBoot to Quarkus
  effort: 1
  labels:
    - konveyor.io/source=springboot
    - konveyor.io/target=quarkus
  links: []
  message: "SpringBoot to Quarkus."
  ruleID: springboot-to-quarkus-00000
  when:
    java.referenced:
      location: ANNOTATION
      pattern: org.springframework.boot.autoconfigure.SpringBootApplication
  order: 1 # New field to allow to sort the instructions to be executed
  instructions: # New section containing the provider's instructions
    ai:
      - promptMessage: "SpringBoot to Quarkus"
    manual:
      - todo: "See openrewrite instructions"
    openrewrite:
      - name: SpringBoot to Quarkus
        description: Move the SpringBootApplication annotation to QuarkusMain, Remove the statement SpringApplication.run(), Add the io.quarkus.runtime.Quarkus.run() method within the main void method and pass the String[] args as parameter"
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
**Step 4**
```yaml
- category: mandatory
  customVariables: []
  description: SpringBoot to Quarkus
  effort: 1
  labels:
    - konveyor.io/source=springboot
    - konveyor.io/target=quarkus
  links: []
  message: "SpringBoot to Quarkus."
  ruleID: springboot-add-class-quarkus-00000
  when:
    java.referenced:
      location: ANNOTATION
      pattern: org.springframework.boot.autoconfigure.SpringBootApplication
  order: 2
  instructions:
    ai:
      - promptMessage: "Add a Quarkus class"
    manual:
      - todo: "See openrewrite instructions"
    openrewrite:
      - name: Add a Quarkus class from class template"
        description: Add a Quarkus class from class template
        recipeList:
          - dev.snowdrop.openrewrite.recipe.spring.CreateJavaClassFromTemplate:
              className: "TodoApplication"
              modifier: "public"
              packageName: "com.todo.app"
              sourceRoot: "src/main/java"
              classTemplate: |
                package %s;
                %sclass %s implements QuarkusApplication {
                    @Override
                    public int run(String... args) throws Exception {
                      System.out.println("Hello user " + args[0]);
                      return 0;
                    }
                }
        gav:
          - dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT
```

