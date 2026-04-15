---
name: ui-model
description: Migrates Spring Boot org.springframework.ui.Model usage to Quarkus Qute Template.data() equivalents within Controller classes.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Migrate Spring Boot Model to Quarkus Qute Template

Use this skill to convert `org.springframework.ui.Model` usage in Spring Boot `@Controller` classes to Quarkus equivalents using Qute templates and `Template.data()`.

## Instructions

- **DO NOT** begin by reading the directory structure or cat-ing files to "understand the project."
- **ASSUME** the project contains Spring Boot `@Controller` classes that use `Model` to pass data to views.
- Examine each Controller file listed in the Source File Inventory.
- Apply the transformations described below.
- Verify the compilation after changes
- Scan for additional occurrences of `org.springframework.ui.Model` across the codebase.

## Phase 1: Identify Model Usage

- [ ] Find all files importing `org.springframework.ui.Model`

## Phase 2: Migrate `org.springframework.ui.Model` to Qute `Template.data()`

### Spring Boot Pattern

In Spring MVC, `org.springframework.ui.Model` is injected as a method parameter in `@Controller` classes to pass data to the view (Thymeleaf, JSP, etc.):

```java
import org.springframework.ui.Model;

@Controller
public class TaskController {

    @GetMapping("/home")
    public String viewHome(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("tasks", taskService.getAllTasks());
        return "home";  // resolves to templates/home.html
    }
}
```

### Quarkus Equivalent

In Quarkus, there is no `Model` object. Instead, you inject a Qute `Template` and pass data using the fluent `.data(key, value)` method, which returns a `TemplateInstance`:

```java
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class TaskResource {

    @Inject
    Template home;  // resolves to src/main/resources/templates/home.html

    @GET
    @Path("/home")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance viewHome() {
        return home.data("task", new Task())
                   .data("tasks", taskRepository.listAll());
    }
}
```

### Migration Steps

- [ ] Remove the `import org.springframework.ui.Model;` statement
- [ ] Add imports:
  ```java
  import io.quarkus.qute.Template;
  import io.quarkus.qute.TemplateInstance;
  import jakarta.inject.Inject;
  ```
- [ ] Inject a `Template` field matching the template name:
  ```java
  @Inject
  Template home;
  ```
- [ ] Remove `Model model` from method parameters
- [ ] Replace each `model.addAttribute("key", value)` with a chained `.data("key", value)` call on the injected template
- [ ] Replace `return "templateName";` with `return templateName.data(...);` returning a `TemplateInstance`
- [ ] If a method returns **both** `TemplateInstance` and `Response` (e.g., for redirects), change the return type to `Object`

### Conversion Table

| Spring Boot (`Model`) | Quarkus (Qute `Template`) |
|---|---|
| `Model model` (method parameter) | Remove parameter; inject `Template` field |
| `model.addAttribute("key", value)` | `template.data("key", value)` |
| `return "home";` | `return home.data(...)` or `return home.instance()` |
| `import org.springframework.ui.Model` | `import io.quarkus.qute.Template` + `import io.quarkus.qute.TemplateInstance` |

## Phase 3: Verification

- [ ] Verify the project compiles: `mvn  compile`
- [ ] Verify no remaining `org.springframework.ui.Model` imports: `grep -r "org.springframework.ui.Model" src/`
- [ ] Check that every template used in `.data()` has a corresponding file in `src/main/resources/templates/`

## Before/After Example

### Before (Spring Boot)

```java
package com.todo.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.todo.app.entity.Task;
import com.todo.app.service.TaskService;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/home")
    public String viewHome(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("tasks", taskService.getAllTasks());
        return "home";
    }

    @GetMapping("/home/{pageNo}")
    public String findPaginated(@PathVariable int pageNo, Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("tasks", taskService.getAllTasks());
        return "home";
    }
}
```

### After (Quarkus)

```java
package com.todo.app.controller;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.todo.app.entity.Task;
import com.todo.app.repository.TaskRepository;
import java.util.List;

@Path("/")
public class TaskResource {

    @Inject
    TaskRepository taskRepository;

    @Inject
    Template home;

    @GET
    @Path("/home")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance viewHome() {
        return home.data("task", new Task())
                   .data("tasks", taskRepository.listAll());
    }

    @GET
    @Path("/home/{pageNo}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance findPaginated(@PathParam("pageNo") int pageNo) {
        List<Task> tasks = taskRepository.listAll();
        return home.data("task", new Task())
                   .data("currentPage", pageNo)
                   .data("tasks", tasks);
    }
}
```

## OpenRewrite Recipes

### Gaps: Recipes NOT Yet Available

The following transformations from this skill have **no existing OpenRewrite recipe** and would need to be implemented as custom recipes or contributed to the `rewrite-spring-to-quarkus` project:

| Transformation | Status | Notes |
|---|---|---|
| `org.springframework.ui.Model` → Qute `Template.data()` | **No recipe exists** | Requires removing `Model` parameters, injecting `Template` fields, converting `model.addAttribute()` to `template.data()`, and changing return types from `String` to `TemplateInstance` |
| `return "templateName"` → `return template.data(...)` | **No recipe exists** | Tied to the Model migration above; requires correlating `model.addAttribute()` calls with the final return statement to build the `.data()` chain |

## Sources

| Source | Description |
|---|---|
| [Qute Templating Engine - Quarkus Guide](https://quarkus.io/guides/qute) | Official Quarkus guide for Qute templates, covering `Template` injection, `.data()` method, and `TemplateInstance` return types |
| [Quarkus REST (formerly RESTEasy Reactive) Guide](https://quarkus.io/guides/resteasy-reactive/) | Official guide for JAX-RS endpoints in Quarkus |

## Dependency Requirements

Ensure the following Quarkus extension is present in `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-qute</artifactId>
</dependency>
```

This extension provides the Qute template engine integration.