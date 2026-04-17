# Spring Boot to Quarkus (Spring Compat) — Recipe Reference

Rules directory: `cookbook/rules/quarkus-spring/`

## Existing rules

| File | RuleID | Order | What it does |
|------|--------|-------|-------------|
| 010-springboot-replace-bom-quarkus.yaml | 010-springboot-replace-bom-quarkus | 1 | Replace Spring Boot parent with Quarkus BOM, add core deps, swap plugins |
| 011-springboot-replace-dependency-jpa.yaml | 011-springboot-replace-dependency-jpa | 2 | Add quarkus-spring-data-jpa + quarkus-jdbc-mysql |
| 012-springboot-replace-dependency-rest-web.yaml | 012-springboot-replace-dependency-rest-web | 3 | Add quarkus-spring-web, quarkus-rest, quarkus-rest-jackson, quarkus-smallrye-health |
| 020-springboot-to-quarkus-rest-annotations.yaml | 020-springboot-to-quarkus-rest-annotations | 4 | @Controller → @RestController |
| 031-springboot-to-quarkus-replace-properties.yaml | 031-springboot-to-quarkus-replace-properties | 6 | Map spring.datasource.* → quarkus.datasource.*, add db-kind, hibernate props |
| 041-springboot-to-quarkus-rest-get-tasks.yaml | 041-springboot-to-quarkus-rest-get-tasks | 7 | Remove method params, replace body, change return type on viewHome() |
| 042-springboot-to-quarkus-redirect.yaml | 042-springboot-to-quarkus-redirect | 8 | Replace redirect method body, change return type to RestResponse |

## Common conditions for this scenario

```yaml
# Trigger on Spring Boot app
java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'

# Trigger on Spring MVC controller
java.annotation is 'org.springframework.stereotype.Controller'

# Trigger on JPA + Spring Data
properties.key is 'spring.jpa.*' AND pom.dependency is (gavs="org.springframework.boot:spring-boot-starter-data-jpa")

# Trigger on Spring web starter
pom.dependency is (gavs="org.springframework.boot:spring-boot-starter-web")

# Trigger on Spring datasource config
properties.key is 'spring.datasource.*'
```

## Quarkus Spring compat dependency mapping

| Spring Starter | Quarkus Equivalent |
|---|---|
| spring-boot-starter-web | quarkus-spring-web + quarkus-rest + quarkus-rest-jackson |
| spring-boot-starter-data-jpa | quarkus-spring-data-jpa + quarkus-jdbc-{db} |
| spring-boot-starter-security | quarkus-spring-security |
| spring-boot-starter-di | quarkus-spring-di |
| spring-boot-starter-scheduled | quarkus-spring-scheduled |
| spring-boot-starter-cache | quarkus-spring-cache |
| spring-boot-starter-actuator | quarkus-smallrye-health + quarkus-micrometer |
| spring-boot-starter-test | quarkus-junit5 |

## Property mapping

| Spring | Quarkus |
|---|---|
| server.port | quarkus.http.port |
| spring.datasource.url | %prod.quarkus.datasource.jdbc.url |
| spring.datasource.username | %prod.quarkus.datasource.username |
| spring.datasource.password | %prod.quarkus.datasource.password |
| spring.datasource.driver-class-name | quarkus.datasource.jdbc.driver |
| spring.jpa.hibernate.ddl-auto | quarkus.hibernate-orm.schema-management.strategy |
| spring.jpa.show-sql | quarkus.hibernate-orm.log.sql |

## GAV coordinates

Standard OpenRewrite:
- `org.openrewrite:rewrite-maven:8.73.0`
- `org.openrewrite:rewrite-java:8.73.0`
- `org.openrewrite.recipe:rewrite-java-dependencies:1.46.0`
- `org.openrewrite.recipe:rewrite-spring-to-quarkus:0.3.1`

Custom mtool:
- `dev.snowdrop.mtool:openrewrite-recipes:1.0.5-SNAPSHOT`

## Next available file number: 060