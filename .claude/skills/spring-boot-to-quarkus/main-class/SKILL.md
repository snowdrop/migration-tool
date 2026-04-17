---
name: main-class
description: Removes the Spring Boot @SpringBootApplication main class (Quarkus generates its own).
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 9: Remove Spring Boot Main Class

Remove the Spring Boot application entry point class since Quarkus generates its own.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains a class annotated with `@SpringBootApplication`. 

If no `@SpringBootApplication` class is found, this skill does not apply.

## Instructions

- Locate the main class.
- Delete the file.

## Migration Steps

- [ ] Delete `AppApplication.java` (or equivalent `@SpringBootApplication` class)
- [ ] Quarkus does not need a main class — it generates one automatically