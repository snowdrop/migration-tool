---
name: build-system
description: Migrates the Maven pom.xml from Spring Boot starter parent and dependencies to Quarkus BOM, extensions, and plugins.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase: Build System (pom.xml)

Migrate the Maven build descriptor from Spring Boot to Quarkus.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] A `pom.xml` exists at the project root
- [ ] The `pom.xml` declares `spring-boot-starter-parent` as parent POM.
- [ ] The `pom.xml` contains one or more `spring-boot-starter-*` dependencies. 
- [ ] The `pom.xml` contains the `spring-boot-maven-plugin`.

If none of the above conditions are met, the build system is not Spring Boot-based and this skill does not apply.

## Instructions

- **DO NOT** begin by reading the directory structure or cat-ing files to "understand the project."
- **ASSUME** the project uses a standard Spring Boot Maven build as confirmed by the preconditions.

## Migration Steps

- [ ] Remove `spring-boot-starter-parent` parent POM
- [ ] Add Quarkus platform BOM (`io.quarkus.platform:quarkus-bom`) via `<dependencyManagement>` using the latest LTS version: 3.33.1.
- [ ] Add `quarkus-maven-plugin` in `<build><plugins>`
- [ ] Replace the Spring Boot starters with their Quarkus equivalent
- [ ] Remove `spring-boot-devtools` as unused
- [ ] Remove `jjwt` as unused
- [ ] Remove `spring-boot-maven-plugin`
- [ ] Remove Spring milestone/snapshot repositories
- [ ] Set Maven compiler source/target to 21
- [ ] Verify the project compiles: `mvn  compile`