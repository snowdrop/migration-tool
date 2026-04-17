# Spring Boot to Native Quarkus — Recipe Reference

Rules directory: `cookbook/rules/quarkus/`

## Existing rules

| File | RuleID | Order | What it does |
|------|--------|-------|-------------|
| 000-springboot-annotation-notfound.yaml | 000-springboot-annotation-notfound | — | Placeholder/test rule for non-existent annotation |
| 001-springboot-replace-bom-quarkus.yaml | 001-springboot-replace-bom-quarkus | 1 | Replace Spring Boot parent with Quarkus BOM, add quarkus-core + quarkus-arc |
| 002-springboot-add-class-quarkus.yaml | 002-springboot-add-class-quarkus | 2 | Create new QuarkusApplication impl class from template |
| 003-springboot-to-quarkus-main-annotation.yaml | 003-springboot-to-quarkus-main-annotation | 3 | Replace @SpringBootApplication with @QuarkusMain, add Quarkus.run() |
| 004-springboot-to-quarkus-rest-annotations.yaml | 004-springboot-to-quarkus-rest-annotations | 4 | TODO — replace Spring MVC annotations with JAX-RS |
| 005-springboot-to-quarkus-rest-annotations-and.yaml | 005-springboot-to-quarkus-rest-annotations-and | 4 | TODO — AND condition variant |

## Common conditions for this scenario

```yaml
# Main trigger
java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'

# Multiple Spring annotations (OR)
java.annotation is 'org.springframework.stereotype.Controller' OR
java.annotation is 'org.springframework.beans.factory.annotation.Autowired' OR
java.annotation is 'org.springframework.web.bind.annotation.GetMapping'

# AND condition
java.annotation is 'org.springframework.stereotype.Controller' AND
java.annotation is 'org.springframework.web.bind.annotation.GetMapping'
```

## Key differences from Spring compat route

- **No Spring compat extensions** — all Spring annotations must be replaced with Quarkus/JAX-RS equivalents
- `@Controller`/`@RestController` → `@Path` + `@GET`/`@POST`/etc.
- `@Autowired` → `@Inject`
- `@SpringBootApplication` → `@QuarkusMain` + `QuarkusApplication`
- `SpringApplication.run()` → `Quarkus.run()`
- Spring Data JPA repos → Panache repositories or active record pattern

## Custom mtool recipes used

- `dev.snowdrop.mtool.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation`
- `dev.snowdrop.mtool.openrewrite.recipe.spring.AddQuarkusRun`
- `dev.snowdrop.mtool.openrewrite.recipe.spring.AddQuarkusMavenPlugin`
- `dev.snowdrop.mtool.openrewrite.recipe.spring.RemoveSpringBootParent`
- `dev.snowdrop.mtool.openrewrite.recipe.java.CreateJavaClassFromTemplate`

## GAV coordinates

- `dev.snowdrop.mtool:openrewrite-recipes:1.0.5-SNAPSHOT`
- `org.openrewrite:rewrite-maven:8.73.0`
- `org.openrewrite:rewrite-java:8.73.0`

## Next available file number: 010