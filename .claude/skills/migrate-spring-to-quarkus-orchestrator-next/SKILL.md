---
name: migrate-spring-to-quarkus-orchestrator
description: Decision-gate orchestrator that evaluates preconditions to determine which migration modules to execute during a Spring Boot to Quarkus migration.
license: Apache-2.0
metadata:
  author: Quarkus Team - https://github.com/quarkusio/quarkus
  version: "0.1.0"
---

# Spring Boot to Quarkus Migration — Orchestrator

This orchestrator uses a **logic gate table** to evaluate preconditions before each phase. A module is executed only when its gate evaluates to **PASS**. Modules with gate **ALWAYS** are mandatory. If a gate evaluates to **SKIP**, the module is bypassed and the orchestrator proceeds to the next.

## Instructions

- Before executing any module, evaluate its **Gate Check** from the Decision Gate Table below.
- If the gate evaluates to **PASS**, read the module's `SKILL.md` and execute its Migration Steps.
- If the gate evaluates to **SKIP**, log the reason and move to the next phase.
- After each phase marked with a compile checkpoint, compile it before proceeding.
- Cross-mark each phase checkbox as you complete or skip it.
- 
## Decision Gate Table

All modules and their SKILL.md are located under:
```
.claude/skills/spring-boot-to-quarkus/<module-folder>/SKILL.md
```

| Domain             | Gate Check (precondition)                                                | Parameters            | Gate Result                          |
|--------------------|--------------------------------------------------------------------------|-----------------------|--------------------------------------|
| `jdk`              | JDK 21+ required                                                         | `VERSION=21`          | **ALWAYS** — stop migration if < 21  |

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
     Read `.claude/skills/migrate-spring-to-quarkus-next/{domain}/SKILL.md`

  4. EXECUTE
     Pass the **Parameters** from the gate table to the loaded skill.
     Follow the Migration Steps defined in the loaded SKILL.md.

  5. When applicable, compile the application
     IF compilation fails --> diagnose and fix before proceeding
     IF compilation passes --> mark checkbox as [x] (done)

  6. LOG result
     Record: "{domain}: DONE"
```

## Execution Checklist

- [ ] **jdk**: Gate = ALWAYS

## Running Individual Modules

To run a single phase, read the specific phase's SKILL.md directly:
- "Read `.claude/skills/migrate-spring-to-quarkus-next/jdk/SKILL.md` and execute it"
- "Run the controller-to-resource phase"
- "Apply the ui-model migration"