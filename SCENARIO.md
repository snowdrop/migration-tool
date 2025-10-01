## Scenario

### From Spring Boot to QuarkusMain and Run

We want to:
1. - Replace the `@SpringBootApplication` annotation with `@QuarkusMain`
2. - Remove the statement `SpringApplication.run(AppApplication.class, args);`
3. - Find the Java Class having as Annotation class: `@QuarkusMain`
   - Check if the class includes a `public void static main()` method
   - Use the main method arguments (String[] args) to pass them to the `Quarkus.run();`
   - Add the import package: `io.quarkus.runtime.Quarkus`

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
