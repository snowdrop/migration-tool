---
name: migrate-spring-to-quarkus
description: Migrate Spring Boot applications to Quarkus. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, or asks about Spring-to-Quarkus migration steps. Supports both Spring compatibility extensions and native Quarkus migration paths.
compatibility: Requires Java 21 and Maven. Optionally uses mtool CLI for automated transformations.
metadata:
  author: snowdrop
  version: "1.0"
---

# Migrate Spring Boot to Quarkus

Guide the user through a structured, phased migration of a Spring Boot application to Quarkus.

## Step 1: Analyze the Spring Boot Application

Scan the application to understand what needs to migrate:

- **Build system**: Read `pom.xml`, identify Spring Boot version, list starters and plugins
- **Java code**: Search for Spring annotations (Core, Web, Data, DI, Security, Test)
- **Configuration**: Read `application.properties`/`application.yml`, check for profiles
- **UI / View layer**: Check for Thymeleaf/JSP templates, static resources, Model+View patterns

Present a summary table with area, findings, and migration complexity. Ask the user to confirm before proceeding.

For detailed dependency and property mappings, see [references/dependency-mappings.md](references/dependency-mappings.md).

## Step 2: Plan the Migration

Propose a phased plan. Each phase maps to a rule execution order:

| Phase | Order | What |
|-------|-------|------|
| 1. Build system | 1 | Replace Spring Boot parent with Quarkus BOM, swap plugins, add core deps |
| 2. Dependencies | 2-3 | Replace Spring starters with Quarkus equivalents |
| 3. Annotations | 4 | Update annotation types (@Controller, @SpringBootTest, etc.) |
| 4. Configuration | 6 | Map Spring properties to Quarkus equivalents, move static resources |
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

2. **Mechanical changes** (deps, properties, type renames): Make directly.

3. **Complex changes** (view layer, REST clients, security): Explain before/after, get user confirmation.

4. **After each phase**, verify: `mvn clean compile -DskipTests`

## Step 4: Verify the Migration

1. `mvn clean package -DskipTests` — build
2. `mvn test` — run tests (must use `@QuarkusTest`)
3. `mvn quarkus:dev` — start the app
4. `curl http://localhost:8080/q/health` — check health endpoint

Present a final migration report table with phase, status, and changes made.

## Important Notes

- **Incremental approach**: Prefer Spring compat extensions first. Migrate to native Quarkus APIs later.
- **Don't break the build**: Verify compilation after each phase.
- **Test port conflict**: If migrating `server.port`, add `quarkus.http.test-port=0` to avoid conflicts with Quarkus test mode.
- **Static resources**: Quarkus serves from `META-INF/resources/`, not `static/`. Move files accordingly.
- **Profile handling**: Spring's `application-{profile}.properties` → Quarkus `%profile.` prefix.