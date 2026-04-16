---
name: migrate-spring-to-quarkus
description: Migrate Spring Boot applications to Quarkus. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, or asks about Spring-to-Quarkus migration steps. Supports both Spring compatibility extensions and native Quarkus migration paths.
compatibility: Requires Java 21 and Maven. Optionally uses mtool CLI for automated transformations.
metadata:
  author: snowdrop
  version: "3.0"
---

# Migrate Spring Boot to Quarkus

Guide the user through a structured, phased migration of a Spring Boot application to Quarkus.

## Critical Rules

- **Never delete code you cannot migrate.** If you cannot fully migrate a piece of code, leave the original in place with a `// TODO: Migration required — <reason>` comment explaining what needs to change and why. This applies to:
  - Methods, classes, or annotations you don't know how to convert
  - Spring-specific patterns without a clear Quarkus equivalent
  - Configuration or wiring code whose purpose is unclear
  If you must remove code (e.g., a Spring-only base class), document what was removed and why in a `// REMOVED:` comment at the same location.
- **Don't break the build.** Run `mvn clean compile -DskipTests` after each phase. Never move to the next phase with a broken build.
- **Document every decision.** When choosing between migration approaches, explain the trade-off to the user.
- **No silent changes.** Every file modification must be intentional and traceable. If a check fails after a phase, diagnose and fix — don't skip the check or delete the failing code.

## Reference Files

Load the relevant reference file before each phase:

| Reference | Use during |
|---|---|
| [references/dependency-map.md](references/dependency-map.md) | Phase 1-2: Build system and dependency mapping |
| [references/annotation-map.md](references/annotation-map.md) | Phase 3-5: Code migration (DI, REST, Data, Security, Test, Lifecycle) |
| [references/config-map.md](references/config-map.md) | Phase 4: Configuration property migration |

## Step 1: Analyze the Spring Boot Application

Scan the application to understand what needs to migrate:

- **Build system**: Read `pom.xml`, identify Spring Boot version, list starters and plugins
- **Java code**: Search for Spring annotations — see [annotation-map.md](references/annotation-map.md) for the full list to check
- **Configuration**: Read `application.properties`/`application.yml`, check for profiles
- **UI / View layer**: Check for Thymeleaf/JSP templates, static resources, Model+View patterns

Present a summary table with area, findings, and migration complexity. Ask the user to confirm before proceeding.

## Step 2: Plan the Migration

Propose a phased plan. Each phase maps to a rule execution order:

| Phase | Order | What |
|-------|-------|------|
| 1. Build system | 1 | Replace Spring Boot parent with Quarkus BOM, swap plugins, add core deps |
| 2. Dependencies | 2-3 | Replace Spring starters with Quarkus equivalents — see [dependency-map.md](references/dependency-map.md) |
| 3. Annotations | 4 | Update annotation types — see [annotation-map.md](references/annotation-map.md) |
| 4. Configuration | 6 | Map Spring properties to Quarkus equivalents — see [config-map.md](references/config-map.md) |
| 5. Complex code | 7-8 | View layer, REST clients, security config — AI-assisted refactoring |

Ask the user which approach they prefer:
- **Spring compat** (recommended): Use `quarkus-spring-web`, `quarkus-spring-data-jpa`, etc. Minimal code changes.
- **Native Quarkus**: Replace all Spring annotations with JAX-RS/CDI. More work, full Quarkus experience.

## Step 3: Execute the Migration

Execute each phase in order:

1. **Prefer mtool when available:**
   ```bash
   mtool analyze <app-path> -r cookbook/rules/quarkus-spring --scanner openrewrite
   mtool transform <app-path> -p openrewrite
   ```

2. **Mechanical changes** (deps, properties, type renames): Make directly using the mapping tables.

3. **Complex changes** (view layer, REST clients, security): Explain before/after, get user confirmation. If you cannot migrate something, add a TODO comment — never silently delete.

4. **After each phase**, verify: `mvn clean compile -DskipTests`

## Step 4: Verify the Migration

Run each check in order. A check fails = stop and fix before continuing.

| # | Check | Command | Pass criteria |
|---|-------|---------|---------------|
| 1 | **Builds** | `mvn clean package -DskipTests` | Exit code 0, no compilation errors |
| 2 | **No Spring deps** | Search `pom.xml` for `org.springframework` | Zero Spring dependencies remaining (except Spring compat extensions if using that strategy) |
| 3 | **Has Quarkus** | Search `pom.xml` for `io.quarkus` | Quarkus BOM and at least one Quarkus extension present |
| 4 | **Tests pass** | `mvn test` | All tests pass using `@QuarkusTest` |
| 5 | **Starts up** | `mvn quarkus:dev` | App starts, `curl http://localhost:8080/q/health` returns UP |
| 6 | **No leftover templates** | Search for Thymeleaf/JSP references | No remaining template engine references (unless intentionally kept with Quarkus extension) |

If any check fails, diagnose the root cause and fix it. Do not skip checks or delete failing code to make them pass.

## Step 5: Migration Review (Self-Reflection)

After completing all checks, review your own migration work. This step improves skill quality over time.

### Review checklist

Answer each question honestly:

1. **What migrated cleanly?** List patterns/areas that mapped 1:1 (e.g., "JPA entities needed no changes", "REST endpoints mapped directly to JAX-RS").
2. **What required manual judgment?** List areas where you had to make non-obvious decisions (e.g., "Replaced Spring Security config with Quarkus properties — no direct equivalent for custom filter chain").
3. **What was left as TODO?** List every `// TODO: Migration required` comment you added and why.
4. **Was any code removed?** If yes, list exactly what was removed and justify each removal. Flag any removal that might cause runtime issues.
5. **What checks failed initially?** List failures from Step 4 and what you did to fix them.
6. **What's missing from the skill references?** Note any mappings (deps, annotations, config) that were missing from the reference files and that you had to figure out.

### Migration Report

Present the review as a structured report:

```
## Migration Report: [app-name]

### Summary
- Strategy: [Full Migration / Spring Compatibility]
- Phases completed: [X/5]
- Checks passed: [X/6]

### Changes by Phase
| Phase | Files changed | Key changes |
|-------|--------------|-------------|
| 1. Build | pom.xml | Replaced Spring Boot parent with Quarkus BOM |
| ... | ... | ... |

### Validation Results
| Check | Result | Notes |
|-------|--------|-------|
| Builds | PASS/FAIL | |
| No Spring deps | PASS/FAIL | |
| Has Quarkus | PASS/FAIL | |
| Tests pass | PASS/FAIL | |
| Starts up | PASS/FAIL | |
| No leftover templates | PASS/FAIL | |

### Unmigrated Code (TODOs)
| File | Line | What | Why not migrated |
|------|------|------|-----------------|

### Removed Code
| File | What was removed | Justification |
|------|-----------------|---------------|

### Skill Improvement Suggestions
- [Any missing mappings, unclear instructions, or edge cases discovered]
```

This report helps the user understand exactly what happened and provides feedback to improve the migration skill for future runs.

## Common Pitfalls

- **Missing `@Transactional`**: Quarkus uses `jakarta.transaction.Transactional`, not Spring's
- **Bean discovery**: Quarkus uses build-time CDI; beans must have a scope annotation
- **No OSIV**: Quarkus doesn't have Open Session in View; lazy loading outside transactions will fail
- **Static resources**: Place in `src/main/resources/META-INF/resources/` (not `static/`)
- **Test port**: Quarkus tests default to port 8081. If app uses 8081, add `quarkus.http.test-port=0`
- **No component scanning**: Beans in external JARs need a Jandex index or `quarkus.index-dependency`
- **Profile handling**: Spring's `application-{profile}.properties` → Quarkus `%profile.` prefix
- **Naming strategy mismatch**: Spring Boot defaults to snake_case (`firstName` → `first_name`). Quarkus/Hibernate 6 preserves camelCase as-is. Set `quarkus.hibernate-orm.physical-naming-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy` to match Spring Boot behavior. **Also update `import.sql`/`data.sql` column names** to match.
- **JAX-RS path conflicts**: Spring allows multiple `@RestController` classes with overlapping `@RequestMapping` paths — JAX-RS does not. When migrating multiple controllers, check for duplicate `@Path` values and consolidate or disambiguate.
- **Qute strict rendering**: Qute defaults to `strict-rendering=true` — missing template variables throw exceptions, unlike Thymeleaf which outputs empty strings. Start migration with `quarkus.qute.strict-rendering=false` and `quarkus.qute.property-not-found-strategy=output-original` to find issues, then enable strict mode.
- **`@InjectMock` package change**: Since Quarkus 3.2, use `io.quarkus.test.InjectMock` (not `io.quarkus.test.junit.mockito.InjectMock`). Old package deprecated in 3.2, removed in 4.0.

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-*` extensions), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-di` | `@Primary`, `@Conditional*`, `@Profile`, `@Lazy` not processed. SpEL `#{...}` in `@Value` throws error. `@Bean` must be inside `@Configuration` class. |
| `quarkus-spring-web` | Only `@RestController` — plain `@Controller` not supported. Only one `@RestControllerAdvice` per app. `@CrossOrigin`, `@InitBinder`, `@ModelAttribute` not supported. No reactive return types (`Mono`, `Flux`). |
| `quarkus-spring-security` | Limited SpEL in `@PreAuthorize`: only `hasRole`, `hasAnyRole`, `permitAll`, `denyAll`, `isAuthenticated`, `@bean.method()`, param comparison. Cannot mix `and`/`or` operators. Cannot combine `@Secured` with `@PreAuthorize`. |
| `quarkus-spring-data-jpa` | SpEL `#{...}` in `@Query` not supported. No `Distinct` queries. Limited custom repository fragment support. |
| `quarkus-spring-cache` | Single cache name only (no arrays). `key`, `condition`, `unless`, `keyGenerator`, `cacheManager` parameters NOT supported. No `@Caching` or `@CacheConfig`. |
| `quarkus-spring-scheduled` | `fixedDelay` NOT supported (only `fixedRate`). Cannot combine `initialDelay` with `cron`. |
| `quarkus-spring-boot-properties` | `@ConstructorBinding` NOT supported (needs no-arg constructor + setters). `Map<K,V>` types NOT supported. |