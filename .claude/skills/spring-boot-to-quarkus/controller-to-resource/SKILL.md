---
name: controller-to-resource
description: Migrates Spring MVC @Controller classes to Quarkus JAX-RS @Path resource classes with annotation and injection replacements.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 6: Controller → JAX-RS Resource

Migrate Spring MVC `@Controller` classes to Quarkus JAX-RS resource classes.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains classes annotated with `@Controller` or `@RestController`.
- [ ] Check for `@RestController` as well
- [ ] Check for Spring Web mapping annotations (`@GetMapping`, `@PostMapping`, etc.)
- [ ] Check for `@Autowired` injection

If the Spring controller and REST annotations are found, this skill does not apply.

## Instructions

- Locate all the Spring controller classes.
- Examine each controller file.
- Apply the annotation and import replacements.

## Migration Steps

- [ ] Replace Spring annotations with JAX-RS + Qute equivalents:

| Spring | Quarkus / Jakarta |
|---|---|
| `@Controller` | `@Path("/")` |
| `@GetMapping("/home")` | `@GET @Path("/home")` |
| `@PostMapping("/home")` | `@POST @Path("/home")` |
| `@DeleteMapping("/{id}")` | `@DELETE @Path("/home/{id}")` |
| `@ResponseBody` | Default behavior in JAX-RS |
| `@PathVariable` | `@RestPath` or `@PathParam` |
| `@RequestBody` | Automatic deserialization |
| `@Autowired` | `@Inject` |

- [ ] Check if the quarkus dependency `quarkus-rest-jackson` is declared part of the pom.xml
- [ ] Replace Spring `Model` with Qute `TemplateInstance` (see `ui-model` skill)
- [ ] Implement redirect for `/` → `/home` using `Response.seeOther(...)` (see `ui-redirect` skill)
- [ ] Implement pagination using Panache's `PanacheQuery`
- [ ] Add `@Transactional` to mutating endpoints (`POST`, `DELETE`)
- [ ] Return `TemplateInstance` for HTML endpoints, JSON for REST endpoints
- [ ] Add `TemplateDateExtensions.java` for `LocalDate` formatting in Qute
- [ ] Add `@Produces(MediaType.APPLICATION_JSON)` and `@Consumes(MediaType.APPLICATION_JSON)` annotations to the Quarkus REST controller class
- [ ] **Ensure all template keys are present in every code path.** Unlike Thymeleaf, which silently treats missing variables as `null`/`false`, Qute throws a `TemplateException` if a key referenced in the template (even inside `{#if}`) is not in the data map. **Every** `.data()` call site must provide the **same complete set of keys**. For example, if the template references `tasks`, `noTasks`, `totalPages`, `currentPage`, and `totalItems`, then:
  - The **empty-result** path must include: `.data("tasks", List.of()).data("totalPages", 0).data("currentPage", 1).data("totalItems", 0L).data("noTasks", true)`
  - The **has-results** path must include: `.data("noTasks", false)` in addition to the actual data values
  
  Forgetting even one key on any path will cause a `TemplateException` at render time.

## Annotation Mapping Reference

| Spring | Quarkus / Jakarta |
|---|---|
| `@SpringBootApplication` | Not needed |
| `@Controller` | `@Path("/")` on resource class |
| `@GetMapping("/path")` | `@GET @Path("/path")` |
| `@PostMapping("/path")` | `@POST @Path("/path")` |
| `@DeleteMapping("/path")` | `@DELETE @Path("/path")` |
| `@ResponseBody` | Default behavior in JAX-RS |
| `@RequestBody` | Automatic deserialization |
| `@PathVariable` | `@RestPath` or `@PathParam` |
| `@Autowired` | `@Inject` |
| `@Service` | `@ApplicationScoped` |
| `@Repository` | `@ApplicationScoped` (with `PanacheRepository`) |
| `Model` (Spring MVC) | `TemplateInstance` (Qute) |