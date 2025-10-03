## Scenario

## From @SpringBootApplication to @QuarkusMain and QuarkusRun

### Introduction

We want to:
0. - Add the Quarkus BOM and dependencies

1. - Replace the `@SpringBootApplication` annotation with `@QuarkusMain`
2. - Remove the statement `SpringApplication.run(AppApplication.class, args);`
3. - Find the Java Class having as Annotation class: `@QuarkusMain`
   - Check if the class includes a `public void static main()` method
   - Find the `method` parameters (String[] args) to pass them to the `Quarkus.run();`
   - Add the import package: `io.quarkus.runtime.Quarkus`
   
4. - Add a Java class implementing QuarkusApplication

### Code to be changed

**Before** we have the `@SpringBootApplication` application 
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

**After**

Pom.xml updated
```xml
        <dependencyManagement>
                <dependencies>
                        <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>3.26.4</version>
                                <type>pom</type>
                                <scope>import</scope>
                        </dependency>
                </dependencies>
        </dependencyManagement>
        <dependencies>
                <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-arc</artifactId>
                </dependency>
                <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-core</artifactId>
                </dependency>
        </dependencies>
```

`Quarkus.run` added to the Application
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
and a new `TodoApplication` class acting as `QuarkusMain` entry point has been created
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

**Steps: 0**
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
  ruleID: springboot-replace-bom-quarkus-0000
  when:
    java.referenced:
      location: ANNOTATION
      pattern: org.springframework.boot.autoconfigure.SpringBootApplication
  order: 1
  instructions:
    ai:
      - promptMessage: "Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file"
    manual:
      - todo: "See openrewrite instructions"
    openrewrite:
      - name: Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file
        description: Replace the SpringBoot parent dependency with Quarkus BOM within the pom.xml file.
        recipeList:
          - org.openrewrite.maven.AddManagedDependency:
              groupId: io.quarkus.platform
              artifactId: quarkus-bom
              version: 3.26.4
              type: pom
              scope: import
              addToRootPom: false
          - org.openrewrite.maven.AddDependency:
              groupId: io.quarkus
              artifactId: quarkus-core
              version: 3.26.4
          - org.openrewrite.maven.AddDependency:
              groupId: io.quarkus
              artifactId: quarkus-arc
              version: 3.26.4
          - org.openrewrite.maven.RemovePlugin:
              groupId: org.springframework.boot
              artifactId: spring-boot-maven-plugin
          - dev.snowdrop.openrewrite.recipe.spring.AddQuarkusMavenPlugin
        gav:
          - dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT
          - org.openrewrite:rewrite-maven:8.62.4
```

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
                import io.quarkus.runtime.QuarkusApplication; 
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

