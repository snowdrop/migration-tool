---
name: validate-migration
description: "Use when user says: validate the migration, check if the migration is correct, compare spring and quarkus projects"
---

## Instructions

Validate a migrated Quarkus project against the original Spring source project using the `quarkit_validate` tool.

### Parameters

- **target** (required): Path to the migrated Quarkus project to validate
- **source**: Path to the original Spring project used as reference (required for `persistence`, `ui` and `all`)
- **validator**: Which validator to run — `project-setup`, `persistence`, `ui`, or `all` (default: `all`)
- **skipCompile**: Set to `true` to skip the build check (default: `false`)
- **parser**: Java parser backend — `treesitter` (default) or `javaparser`
- **uiMigrationType**: UI migration type(s), required by `ui` — see [UI migration types](#ui-migration-types). With `all`, the `ui` validator only runs when it is set

### Available validators

1. **project-setup** — Checks the Quarkus project structure:
   - `pom.xml` exists with proper coordinates
   - `application.properties` exists
   - No remaining Spring Boot annotations (`@SpringBootApplication`, etc.)
   - No Spring dependencies in `pom.xml`
   - `quarkus-maven-plugin` and `quarkus-bom` are present
   - Standard Maven directories exist
   - `mvn compile` succeeds (unless `skipCompile` is `true`)

2. **persistence** — Compares JPA entities between Spring and Quarkus:
   - Entity count matches
   - All Spring entities exist in Quarkus
   - Table names match (handles constant references as warnings)
   - ID generation strategy is preserved (detects PanacheEntity defaults)
   - All fields are present with matching types and column mappings
   - Relationships (`@OneToMany`, `@ManyToOne`, etc.) are preserved
   - The project compiles with Maven or Gradle (unless `skipCompile` is `true`)

3. **ui** — Checks the view layer migration for the given `uiMigrationType`:
   - Static resources are in `META-INF/resources/` and Spring CSRF tokens are removed
   - The legacy view technology is removed (files, tags, expressions, dependencies, Java classes)
   - Qute setup (`quarkus-rest-qute`, templates, `@CheckedTemplate` locations, message bundles, JAX-RS resources), or the FreeMarker or MyFaces setup when the technology is preserved
   - The target has as many views as the source
   - The Quarkus build succeeds (unless `skipCompile` is `true`)

### UI migration types

| `uiMigrationType` | Migration |
|---|---|
| `jsp-qute` | JSP to Qute |
| `thymeleaf-qute` | Thymeleaf to Qute |
| `freemarker-qute` | FreeMarker to Qute |
| `freemarker-quarkus` | FreeMarker preserved with `quarkus-freemarker` |
| `jsf-qute` | JSF to Qute |
| `jsf-myfaces` | JSF preserved with MyFaces |

Use a comma-separated list when the project mixes view technologies (e.g. `thymeleaf-qute,jsp-qute`). A technology is either migrated to Qute or preserved: `freemarker-qute` cannot be combined with `freemarker-quarkus`, nor `jsf-qute` with `jsf-myfaces`.

### Example usage

Validate everything:
```
quarkit_validate(target="/path/to/quarkus-app", source="/path/to/spring-app")
```

Validate only persistence, skip compile:
```
quarkit_validate(target="/path/to/quarkus-app", source="/path/to/spring-app", validator="persistence", skipCompile="true")
```

Validate the view layer of a Thymeleaf application:
```
quarkit_validate(target="/path/to/quarkus-app", source="/path/to/spring-app", validator="ui", uiMigrationType="thymeleaf-qute")
```

## Output format

- Report results grouped by validator name
- Each check shows status: `[PASS]`, `[FAIL]`, or `[WARN]`
- Include the rule name and evidence for each result
- Summarize total rules, passed, failed, and warnings
- If failures are found, suggest specific fixes based on the evidence
