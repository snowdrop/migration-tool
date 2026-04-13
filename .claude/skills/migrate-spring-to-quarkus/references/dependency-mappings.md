# Dependency and Property Mappings

## Spring Compatibility Extensions (incremental migration)

| Spring Starter | Quarkus Dependency | Notes |
|---|---|---|
| spring-boot-starter-web | quarkus-spring-web + quarkus-rest + quarkus-rest-jackson | Spring MVC annotations work on Quarkus |
| spring-boot-starter-data-jpa | quarkus-spring-data-jpa + quarkus-jdbc-{db} | Spring Data repos work as-is |
| spring-boot-starter-security | quarkus-spring-security | Basic Spring Security annotations |
| spring-boot-starter-di | quarkus-spring-di | @Autowired, @Component, etc. |
| spring-boot-starter-scheduled | quarkus-spring-scheduled | @Scheduled support |
| spring-boot-starter-cache | quarkus-spring-cache | @Cacheable support |
| spring-boot-starter-actuator | quarkus-smallrye-health + quarkus-micrometer | Health + metrics |
| spring-boot-starter-validation | quarkus-hibernate-validator | Bean validation |
| spring-boot-starter-test | quarkus-junit5 | Replace @SpringBootTest with @QuarkusTest |

## Native Quarkus (full migration)

| Spring Starter | Quarkus Dependency | Code changes required |
|---|---|---|
| spring-boot-starter-web | quarkus-rest + quarkus-rest-jackson | Replace Spring MVC with JAX-RS |
| spring-boot-starter-data-jpa | quarkus-hibernate-orm-panache | Rewrite repos to Panache |
| spring-boot-starter-security | quarkus-oidc or quarkus-security-jpa | Rewrite security config |

## Annotation Changes

| Spring | Quarkus (Spring compat) | Quarkus (native) |
|---|---|---|
| @Controller | @RestController | @Path |
| @SpringBootApplication | Keep (compat) | @QuarkusMain |
| @SpringBootTest | @QuarkusTest | @QuarkusTest |
| @MockBean | @InjectMock | @InjectMock |
| @Autowired | Keep (compat) | @Inject |

## Property Mapping

| Spring Property | Quarkus Property |
|---|---|
| server.port | quarkus.http.port |
| spring.datasource.url | %prod.quarkus.datasource.jdbc.url |
| spring.datasource.username | %prod.quarkus.datasource.username |
| spring.datasource.password | %prod.quarkus.datasource.password |
| spring.datasource.driver-class-name | quarkus.datasource.jdbc.driver |
| spring.jpa.hibernate.ddl-auto | quarkus.hibernate-orm.schema-management.strategy |
| spring.jpa.show-sql | quarkus.hibernate-orm.log.sql |
| spring.jpa.database-platform | quarkus.datasource.db-kind |
| logging.level.* | quarkus.log.category."*".level |

## Complex Code Migrations (AI-assisted)

| Spring Pattern | Quarkus Equivalent |
|---|---|
| Spring MVC Model+View (Thymeleaf/JSP) | Qute templates with TemplateInstance |
| RestTemplate | quarkus-rest-client with @RegisterRestClient |
| @ConfigurationProperties | @ConfigMapping |
| ApplicationRunner / CommandLineRunner | @Observes StartupEvent |
| WebSecurityConfigurerAdapter | Quarkus security config |
| @Async | @NonBlocking / Mutiny |