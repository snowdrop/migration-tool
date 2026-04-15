---
name: verification
description: Final verification and cleanup after Spring Boot to Quarkus migration — compile, run, test, remove leftover Spring artifacts.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 11: Verification & Cleanup

Final verification that the migration is complete and no Spring Boot artifacts remain.

## Preconditions

This skill should be executed **after all other migration phases are complete**. It has no skip condition — always run it as the final step.

- [ ] All previous migration phases (build-system through testing) have been executed

## Instructions

- Use maven build tool to run compilation, dev mode, and dependency checks.
- Scan for leftover Spring imports and artifacts.

## Verification Steps

- [ ] Verify project compiles: `mvn compile`
- [ ] Verify project starts in dev mode: `mvn quarkus:dev`
- [ ] Test page rendering at `http://localhost:8080/home`
- [ ] Test task creation via the form (POST `/home`)
- [ ] Test task deletion via the delete button (DELETE `/home/{id}`)
- [ ] Test pagination with > 6 tasks
- [ ] Test error page rendering

## Cleanup Steps

- [ ] Remove any leftover Spring imports from all Java files
- [ ] Verify no Spring dependencies remain in the dependency tree:
  ```bash
  mvn  dependency:tree | grep springframework
  ```

## Token cost and carbon footprint

- [ ] Estimate and report the token usage based on the files processed. Use the average cost of Claude 4.6 Opus ($12/$60) to provide a cost estimate and use a factor of 0.2g CO2​e per 1k tokens for the environmental impact report.


## Project Summary

After a successful migration, the project should match:

| | Spring Boot (before) | Quarkus (after) |
|---|---|---|
| **Framework** | Spring Boot 3.5.3 | Quarkus 3.33.1 LTS |
| **Java** | 21 | 21 |
| **Web** | Spring MVC (`@Controller`) | Quarkus REST (`@Path`, JAX-RS) + Qute |
| **ORM** | Spring Data JPA (`JpaRepository`) | Hibernate ORM Panache (`PanacheRepository`) |
| **Templates** | Thymeleaf | Qute |
| **Database** | MySQL 8 | MySQL 8 |
| **Build** | spring-boot-maven-plugin | quarkus-maven-plugin |