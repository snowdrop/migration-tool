---
name: configuration
description: Migrates Spring Boot application.properties to Quarkus equivalents (datasource, HTTP port, Hibernate settings).
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 2: Configuration (application.properties)

Migrate Spring Boot configuration properties to their Quarkus equivalents.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] Check if `src/main/resources/application.properties` (or `application.yml`) exists and contains Spring Boot-specific properties. 

If no Spring Boot properties are found, this skill does not apply.

## Instructions

- Examine `src/main/resources/application.properties`.
- Replace Spring properties with Quarkus equivalents.
- Do not remove properties that are framework-agnostic (e.g., custom app properties).

## Migration Steps

- [ ] Map Spring properties to Quarkus equivalents:

| Spring Boot property | Quarkus property                                                   |
|---|--------------------------------------------------------------------|
| `server.port` | `quarkus.http.port`                                                |
| `spring.datasource.url` | `quarkus.datasource.jdbc.url`                                      |
| `spring.datasource.username` | `quarkus.datasource.username`                                      |
| `spring.datasource.password` | `quarkus.datasource.password`                                      |
| `spring.datasource.driver-class-name` | Remove (inferred from `quarkus.datasource.db-kind`)                |
| `spring.jpa.hibernate.ddl-auto` | `quarkus.hibernate-orm.schema-management.strategy=drop-and-create` |
| `spring.jpa.show-sql` | `quarkus.hibernate-orm.log.sql`                                    |
| `spring.jpa.properties.hibernate.dialect` | Remove (inferred from `db-kind`)                                   |

- [ ] Add `quarkus.datasource.db-kind=mysql` (or appropriate DB kind)
- [ ] Replace the server port `8081` to `8080`