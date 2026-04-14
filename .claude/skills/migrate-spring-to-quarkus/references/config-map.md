# Spring Boot to Quarkus Configuration Map

## Server

| Spring Boot | Quarkus |
|---|---|
| `server.port=8080` | `quarkus.http.port=8080` |
| `server.servlet.context-path=/api` | `quarkus.http.root-path=/api` |
| `server.ssl.key-store` | `quarkus.http.ssl.certificate.key-store-file` |
| `server.compression.enabled=true` | `quarkus.http.enable-compression=true` |
| `server.error.include-message=always` | Configure via exception mappers |

## Datasource

| Spring Boot | Quarkus |
|---|---|
| `spring.datasource.url` | `quarkus.datasource.jdbc.url` |
| `spring.datasource.username` | `quarkus.datasource.username` |
| `spring.datasource.password` | `quarkus.datasource.password` |
| `spring.datasource.driver-class-name` | `quarkus.datasource.db-kind` (auto-detected) |

## JPA / Hibernate

| Spring Boot | Quarkus |
|---|---|
| `spring.jpa.hibernate.ddl-auto=update` | `quarkus.hibernate-orm.database.generation=update` |
| `spring.jpa.show-sql=true` | `quarkus.hibernate-orm.log.sql=true` |
| `spring.jpa.properties.hibernate.dialect` | `quarkus.hibernate-orm.dialect` (usually auto-detected) |
| `spring.jpa.properties.hibernate.format_sql` | `quarkus.hibernate-orm.log.format-sql=true` |
| `spring.jpa.open-in-view=false` | Not applicable (no OSIV in Quarkus) |
| `spring.jpa.defer-datasource-initialization` | Use Flyway or `import.sql` |

## Flyway

| Spring Boot | Quarkus |
|---|---|
| `spring.flyway.enabled=true` | `quarkus.flyway.migrate-at-start=true` |
| `spring.flyway.locations=classpath:db/migration` | `quarkus.flyway.locations=db/migration` |
| `spring.flyway.baseline-on-migrate=true` | `quarkus.flyway.baseline-on-migrate=true` |

## Logging

| Spring Boot | Quarkus |
|---|---|
| `logging.level.root=INFO` | `quarkus.log.level=INFO` |
| `logging.level.com.example=DEBUG` | `quarkus.log.category."com.example".level=DEBUG` |
| `logging.file.name=app.log` | `quarkus.log.file.enable=true` + `quarkus.log.file.path=app.log` |
| `logging.pattern.console` | `quarkus.log.console.format` |

## Profiles

| Spring Boot | Quarkus |
|---|---|
| `application-{profile}.properties` | `application-{profile}.properties` (same convention) |
| `spring.profiles.active=dev` | `quarkus.profile=dev` or `-Dquarkus.profile=dev` |
| `@Profile("dev")` | `@IfBuildProfile("dev")` |
| `application-test.properties` | `%test.` prefix in `application.properties`, or `application-test.properties` |

## CORS

| Spring Boot | Quarkus |
|---|---|
| `@CrossOrigin` or `WebMvcConfigurer` | `quarkus.http.cors=true` |
| -- | `quarkus.http.cors.origins=http://localhost:3000` |
| -- | `quarkus.http.cors.methods=GET,POST,PUT,DELETE` |

## Cache

| Spring Boot | Quarkus |
|---|---|
| `spring.cache.type=caffeine` | Extension `quarkus-cache` (Caffeine-based by default) |
| `@Cacheable("name")` | `@io.quarkus.cache.CacheResult(cacheName = "name")` |
| `@CacheEvict("name")` | `@io.quarkus.cache.CacheInvalidate(cacheName = "name")` |

## Security

| Spring Boot | Quarkus |
|---|---|
| `spring.security.user.name` | `quarkus.security.users.embedded.users.<name>.password` |
| `spring.security.oauth2.client.*` | `quarkus.oidc.*` |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `quarkus.oidc.auth-server-url` |

## Actuator / Health

| Spring Boot | Quarkus |
|---|---|
| `management.endpoints.web.exposure.include=*` | Endpoints auto-exposed at `/q/` |
| `management.endpoint.health.show-details=always` | `quarkus.smallrye-health.ui.always-include=true` |
| `/actuator/health` | `/q/health` |
| `/actuator/metrics` | `/q/metrics` |
| `/actuator/info` | `/q/info` (with `quarkus-info`) |

## Static Resources

| Spring Boot | Quarkus |
|---|---|
| `src/main/resources/static/` | `src/main/resources/META-INF/resources/` |
| `src/main/resources/public/` | `src/main/resources/META-INF/resources/` |
| `spring.web.resources.static-locations` | Quarkus always uses `META-INF/resources/` |

## Spring Cloud Config Server

| Spring Boot | Quarkus (`quarkus-spring-cloud-config-client`) |
|---|---|
| `spring.cloud.config.uri` | `quarkus.spring-cloud-config.url` (default: `http://localhost:8888`) |
| `spring.cloud.config.name` | `quarkus.spring-cloud-config.name` |
| `spring.cloud.config.label` | `quarkus.spring-cloud-config.label` |
| `spring.cloud.config.username` | `quarkus.spring-cloud-config.username` |
| `spring.cloud.config.password` | `quarkus.spring-cloud-config.password` |
| `spring.cloud.config.fail-fast` | `quarkus.spring-cloud-config.fail-fast` (default: false) |
| `spring.profiles.active` | `quarkus.spring-cloud-config.profiles` |

## Spring Extension Toggle Properties

Each Spring compat extension can be disabled at build time:

| Property | Default | Effect |
|---|---|---|
| `quarkus.spring-di.enabled` | `true` | Disable Spring DI annotation processing |
| `quarkus.spring-cache.enabled` | `true` | Disable Spring Cache annotation processing |