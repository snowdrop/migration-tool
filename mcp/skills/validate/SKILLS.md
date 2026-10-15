---
name: validate-migration
description: "Use when user says: validate the migration, check if the migration is correct, compare spring and quarkus projects"
---

## Instructions

Validate a migrated Quarkus project against the original Spring source project using the `sourceValidate` tool.

### Parameters

- **target** (required): Path to the migrated Quarkus project to validate
- **source** (required): Path to the original Spring project used as reference
- **validator**: Which validator to run — `project-setup`, `persistence`, or `all` (default: `persistence`)
- **skipCompile**: Set to `true` to skip the `mvn compile` check (default: `false`)
- **parser**: Java parser backend — `treesitter` or `javaparser` (default)

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

### Example usage

Validate everything:
```
sourceValidate(target="/path/to/quarkus-app", source="/path/to/spring-app")
```

Validate only persistence, skip compile:
```
sourceValidate(target="/path/to/quarkus-app", source="/path/to/spring-app", validator="persistence", skipCompile="true")
```

## Output format

- Report results grouped by validator name
- Each check shows status: `[PASS]`, `[FAIL]`, or `[WARN]`
- Include the rule name and evidence for each result
- Summarize total rules, passed, failed, and warnings
- If failures are found, suggest specific fixes based on the evidence
