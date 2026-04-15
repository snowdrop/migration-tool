---
name: ui-redirect
description: Migrates Spring Boot redirect statements (return "redirect:/...") to Quarkus JAX-RS Response.seeOther() equivalents within Controller classes.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Migrate Spring Boot Redirect to Quarkus Response.seeOther()

Use this skill to convert `return "redirect:/..."` statements in Spring Boot `@Controller` classes to their Quarkus equivalents using JAX-RS `Response.seeOther()`.

## Instructions

- **DO NOT** begin by reading the directory structure or cat-ing files to "understand the project."
- **ASSUME** the project contains Spring Boot `@Controller` classes that use `redirect:` return values.
- Examine each Controller file listed in the Source File Inventory.
- Apply the transformations described below.
- Verify and compile after changes.
- scan for additional occurrences of `"redirect:"` across the codebase.

## Phase 1: Identify Redirect Usage

- [ ] Use `Grep` to find all files containing `return "redirect:`:
  ```
  grep -r 'return "redirect:' src/
  ```

## Phase 2: Migrate `return "redirect:/..."` to `Response.seeOther()`

### Spring Boot Pattern

In Spring MVC, returning a string prefixed with `"redirect:"` triggers an HTTP 302 redirect:

```java
@GetMapping("/")
public String viewIndexPage() {
    return "redirect:/home";
}

@GetMapping("/home/{pageNo}")
public String findPaginated(@PathVariable int pageNo, Model model) {
    if (pageNo < 1) {
        return "redirect:/home";
    }
    if (pageNo > totalPages) {
        return "redirect:/home/" + totalPages;
    }
    // ... normal template rendering
    return "home";
}
```

### Quarkus Equivalent

In Quarkus (JAX-RS), use `Response.seeOther(URI)` to issue an HTTP 303 redirect. The 303 status code is semantically correct for redirecting after a GET request to another resource:

```java
import jakarta.ws.rs.core.Response;
import java.net.URI;

@GET
@Path("/")
public Response viewIndexPage() {
    return Response.seeOther(URI.create("/home")).build();
}

@GET
@Path("/home/{pageNo}")
@Produces(MediaType.TEXT_HTML)
public Object findPaginated(@PathParam("pageNo") int pageNo) {
    if (pageNo < 1) {
        return Response.seeOther(URI.create("/home")).build();
    }
    if (pageNo > totalPages) {
        return Response.seeOther(URI.create("/home/" + totalPages)).build();
    }
    // ... normal template rendering
    return home.data("tasks", tasks);
}
```

### Migration Steps

- [ ] Add imports:
  ```java
  import jakarta.ws.rs.core.Response;
  import java.net.URI;
  ```
- [ ] Replace each `return "redirect:/path";` with `return Response.seeOther(URI.create("/path")).build();`
- [ ] For dynamic paths like `return "redirect:/home/" + pageNo;`, use `return Response.seeOther(URI.create("/home/" + pageNo)).build();`
- [ ] If the method now returns both `TemplateInstance` and `Response`, change the return type to `Object`

### Conversion Table

| Spring Boot (redirect) | Quarkus (JAX-RS Response) |
|---|---|
| `return "redirect:/home";` | `return Response.seeOther(URI.create("/home")).build();` |
| `return "redirect:/home/" + page;` | `return Response.seeOther(URI.create("/home/" + page)).build();` |
| Return type: `String` | Return type: `Response` (or `Object` if mixed with `TemplateInstance`) |
| HTTP 302 Found | HTTP 303 See Other |

## Phase 3: Verification

- [ ] Verify the project compiles: `mvn  compile`
- [ ] Verify no remaining `"redirect:"` returns: `grep -r 'return "redirect:' src/`

## Before/After Example

### Before (Spring Boot)

```java
package com.todo.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TaskController {

    @GetMapping("/")
    public String viewIndexPage() {
        return "redirect:/home";
    }

    @GetMapping("/home/{pageNo}")
    public String findPaginated(@PathVariable int pageNo) {
        if (pageNo < 1) {
            return "redirect:/home";
        }
        return "home";
    }
}
```

### After (Quarkus)

```java
package com.todo.app.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class TaskResource {

    @GET
    public Response viewIndexPage() {
        return Response.seeOther(URI.create("/home")).build();
    }

    @GET
    @Path("/home/{pageNo}")
    public Object findPaginated(@PathParam("pageNo") int pageNo) {
        if (pageNo < 1) {
            return Response.seeOther(URI.create("/home")).build();
        }
        return home.data("tasks", tasks);
    }
}
```

## OpenRewrite Recipes

### Gaps: Recipes NOT Yet Available

| Transformation | Status | Notes |
|---|---|---|
| `return "redirect:/path"` → `Response.seeOther(URI.create("/path")).build()` | **No recipe exists** | Requires parsing the `"redirect:"` prefix from return strings, constructing `URI.create()` calls with the extracted path, and changing return types to `Response` or `Object` |

### How to Run Related Available Recipes

```bash
# Run the ResponseEntity conversion (related but does not cover redirect: strings)
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring-to-quarkus:RELEASE \
  -Drewrite.activeRecipes=org.openrewrite.quarkus.spring.ResponseEntityToJaxRsResponse
```

## Sources

| Source | Description |
|---|---|
| [Quarkus REST (formerly RESTEasy Reactive) Guide](https://quarkus.io/guides/resteasy-reactive/) | Official guide for JAX-RS endpoints in Quarkus, including `Response` handling |
| [JAX-RS Response#seeOther - Adam Bien](https://www.adam-bien.com/roller/abien/entry/jax_rs_response_seeother_vs) | Explains the difference between `Response.seeOther()` (303) and other redirect status codes (301, 302, 307, 308) |
| [Mastering HTTP Responses in Quarkus](https://www.the-main-thread.com/p/quarkus-http-response-guide-java-developers) | Guide covering fine-grained HTTP response control in Quarkus including redirects |
| [Convert Spring ResponseEntity to JAX-RS Response - OpenRewrite](https://docs.openrewrite.org/recipes/quarkus/spring/responseentitytojaxrsresponse) | OpenRewrite recipe converting `ResponseEntity` patterns to JAX-RS `Response` |