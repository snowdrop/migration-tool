---
name: spring-boot-to-quarkus-migration-orchestrator
description: Decision-gate orchestrator that evaluates preconditions to determine which migration phases to execute during a Spring Boot to Quarkus migration.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Spring Boot to Quarkus Migration — Orchestrator

This orchestrator uses a **logic gate table** to evaluate preconditions before each phase. A phase is executed only when its gate evaluates to **PASS**. Phases with gate **ALWAYS** are mandatory. If a gate evaluates to **SKIP**, the phase is bypassed and the orchestrator proceeds to the next.

## Instructions

- **DO NOT** begin by reading the directory structure or cat-ing files to "understand the project."
- Before executing any phase, evaluate its **Gate Check** from the Decision Gate Table below.
- If the gate evaluates to **PASS**, read the phase's `SKILL.md` and execute its Migration Steps.
- If the gate evaluates to **SKIP**, log the reason and move to the next phase.
- After each phase marked with a compile checkpoint, run `./mvnw compile` before proceeding.
- Cross-mark each phase checkbox as you complete or skip it.

## Decision Gate Table

All phase skills are located under:
```
.claude/skills/spring-boot-to-quarkus/<phase-folder>/SKILL.md
```

| Domain                   | Gate Check (precondition)                                                                                                               | Gate Result                                                             |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `check-jdk`              | JDK 21+ required                                                                                                                        | Check java version                                                      | **ALWAYS** — stop migration if < 21 |
| `maven-build-system`     | `pom.xml` declares `spring-boot-starter-parent` as parent, contains `spring-boot-starter-*` dependencies, or `spring-boot-maven-plugin` | **PASS** if any Spring Boot marker found; **SKIP** otherwise            |
| `configuration`          | `application.properties` or `application.yml` exists and contains `spring.` or `server.` properties                                     | **PASS** if Spring properties found; **SKIP** otherwise                 |
| `entity-layer`           | Classes annotated with `@Entity` exist AND do NOT already extend `PanacheEntity`                                                        | **PASS** if `@Entity` found without `PanacheEntity`; **SKIP** otherwise |
| `repository-layer`       | Interfaces extending `JpaRepository` or `CrudRepository` exist                                                                          | **PASS** if Spring Data repos found; **SKIP** otherwise                 |
| `service-layer`          | Classes annotated with `@Service` exist                                                                                                 | **PASS** if `@Service` found; **SKIP** otherwise                        |
| `controller-to-resource` | Classes annotated with `@Controller` or `@RestController` exist                                                                         | **PASS** if Spring controllers found; **SKIP** otherwise                |
| `ui-model`               | Controller classes use `org.springframework.ui.Model` parameter                                                                         | **PASS** if `Model` usage found; **SKIP** otherwise                     |
| `ui-redirect`            | Controller methods return `"redirect:/"` strings                                                                                        | **PASS** if redirect patterns found; **SKIP** otherwise                 |
| `templates`              | Template files exist in `src/main/resources/templates/` with Thymeleaf `th:` syntax                                                     | **PASS** if Thymeleaf templates found; **SKIP** otherwise               |
| `static-assets`          | Static assets exist in `src/main/resources/static/`                                                                                     | **PASS** if static dir has content; **SKIP** otherwise                  |
| `main-class`             | A class annotated with `@SpringBootApplication` exists                                                                                  | **PASS** if main class found; **SKIP** otherwise                        |
| `testing`                | Test classes annotated with `@SpringBootTest` exist                                                                                     | **PASS** if Spring Boot tests found; **SKIP** otherwise                 |
| `verification`           | All previous phases completed                                                                                                           | **ALWAYS** — final cleanup and compilation check                        |

## Execution Protocol

For each phase, follow this protocol:

```
  1. EVALUATE the gate check according to the precondition

  2. DECIDE
     IF gate == ALWAYS  --> proceed to step 3
     IF gate == PASS    --> proceed to step 3
     IF gate == SKIP    --> log "{domain}: SKIPPED — {reason}"
                            mark checkbox as [x] (skipped)
                            CONTINUE to next phase

  3. LOAD skill
     Read `.claude/skills/migration/spring-boot-to-quarkus/{domain}/SKILL.md`

  4. EXECUTE
     Follow the Migration Steps defined in the loaded SKILL.md.

  5. When applicable, compile the application
     IF compilation fails --> diagnose and fix before proceeding
     IF compilation passes --> mark checkbox as [x] (done)

  6. LOG result
     Record: "{domain}: DONE"
```

## Execution Checklist

- [ ] **check-jdk**: Gate = ALWAYS
- [ ] **maven-build-system**: Gate = `spring-boot-starter-parent` in pom.xml?
- [ ] **configuration**: Gate = `spring.*` or `server.*` properties?
- [ ] **entity-layer**: Gate = `@Entity` without `PanacheEntity`?
- [ ] **repository-layer**: Gate = `JpaRepository` or `CrudRepository`?
- [ ] **service-layer**: Gate = `@Service`?
- [ ] **controller-to-resource**: Gate = `@Controller` or `@RestController`?
- [ ] **ui-model**: Gate = `org.springframework.ui.Model` usage?
- [ ] **ui-redirect**: Gate = `redirect:/` return values?
- [ ] **templates**: Gate = Thymeleaf `th:` syntax in templates?
- [ ] **static-assets**: Gate = `static/` directory has content?
- [ ] **main-class**: Gate = `@SpringBootApplication`?
- [ ] **testing**: Gate = `@SpringBootTest`?
- [ ] **verification**: Gate = ALWAYS

## Phase Dependency Graph

Phases must execute in order. Some have hard dependencies on prior phases:

```
check-jdk .............. (no dependency)
maven-build-system ..... (no dependency)
configuration .......... (no dependency)
entity-layer ........... (no dependency)
repository-layer ....... (requires entity-layer)
service-layer .......... (requires repository-layer)
controller-to-resource . (requires layers migrated)
ui-model ............... (requires controllers classes converted)
ui-redirect ............ (requires controllers classes converted)
templates .............. (requires resource classes ready)
static-assets .......... (no dependency)
main-class ............. (no dependency)
testing ................ (requires migration completed)
verification ........... (requires all phases attempted)
```

## Running Individual Phases

To run a single phase, read the specific phase's SKILL.md directly:
- "Read `.claude/skills/migration/spring-boot-to-quarkus/maven-build-system/SKILL.md` and execute it"
- "Run the controller-to-resource phase"
- "Apply the ui-model migration"