---
name: testing
description: Migrates Spring Boot @SpringBootTest test classes to Quarkus @QuarkusTest with REST Assured.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 10: Testing

Migrate Spring Boot test classes to Quarkus test framework.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains test classes annotated with `@SpringBootTest`.

If no Spring Boot test classes are found, this skill does not apply.

## Instructions

- Locate all test classes.
- Examine each test file.

## Migration Steps

- [ ] Rewrite `AppApplicationTests.java` using `@QuarkusTest`:
  ```java
  import io.quarkus.test.junit.QuarkusTest;
  import org.junit.jupiter.api.Test;
  import static io.restassured.RestAssured.given;

  @QuarkusTest
  class AppApplicationTests {
      @Test
      void homePageLoads() {
          given().when().get("/home").then().statusCode(200);
      }
  }
  ```
- [ ] Ensure `quarkus-junit` and `rest-assured` test dependencies are in `pom.xml`
- [ ] Remove Spring test imports (`org.springframework.boot.test.*`)