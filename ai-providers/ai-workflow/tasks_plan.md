# Spring Boot to Quarkus Migration Plan

## Project Summary

| | Spring Boot (before) | Quarkus (after) |
|---|---|---|
| **Framework** | Spring Boot 3.5.3 | Quarkus 3.33.1 LTS |
| **Java** | 21 | 21 |
| **Web** | Spring MVC (`@Controller`) | Quarkus REST (`@Path`, JAX-RS) + Qute |
| **ORM** | Spring Data JPA (`JpaRepository`) | Hibernate ORM Panache (`PanacheRepository`) |
| **Templates** | Thymeleaf | Qute |
| **Database** | MySQL 8 | MySQL 8 |
| **Build** | spring-boot-maven-plugin | quarkus-maven-plugin |

## Source File Inventory

| Spring Boot file | Migration action | Status |
|---|---|---|
| `pom.xml` | Replace parent, BOM, dependencies, plugins | Done |
| `AppApplication.java` | Remove (no main class needed in Quarkus) | Done |
| `Task.java` (entity) | Extend `PanacheEntity`, switch to public fields | Done |
| `TaskRepository.java` | Replace with `PanacheRepository` | Done |
| `TaskService.java` (interface) | Remove (unnecessary abstraction) | Done |
| `TaskServiceImpl.java` | Merge into `TaskRepository` or resource | Done |
| `TaskController.java` | Rewrite as JAX-RS `@Path` resource with Qute | Done |
| `application.properties` | Remap to Quarkus property keys | Done |
| `home.html` (Thymeleaf) | Rewrite as Qute template | Done |
| `error.html` (Thymeleaf) | Rewrite as Qute template | Done |
| `home.js` | Update (remove CSRF handling, adjust endpoints) | Done |
| `AppApplicationTests.java` | Rewrite with `@QuarkusTest` | Done |

---

## Phase 1: Build System (pom.xml)

- [x] Remove `spring-boot-starter-parent` parent POM
- [x] Add Quarkus platform BOM (`io.quarkus.platform:quarkus-bom:3.33.1`) via `<dependencyManagement>`
- [x] Add `quarkus-maven-plugin` in `<build><plugins>`
- [x] Replace dependencies:
  - [x] `spring-boot-starter-web` → `io.quarkus:quarkus-rest` + `io.quarkus:quarkus-rest-jackson`
  - [x] `spring-boot-starter-data-jpa` → `io.quarkus:quarkus-hibernate-orm-panache`
  - [x] `spring-boot-starter-thymeleaf` → `io.quarkus:quarkus-rest-qute`
  - [x] `mysql-connector-j` → `io.quarkus:quarkus-jdbc-mysql`
  - [x] `spring-boot-starter-test` → `io.quarkus:quarkus-junit` + `io.rest-assured:rest-assured`
  - [x] `spring-boot-devtools` → remove (Quarkus has built-in dev mode)
  - [x] `jjwt` → remove (unused)
- [x] Remove `spring-boot-maven-plugin`
- [x] Remove Spring milestone/snapshot repositories
- [x] Set Maven compiler source/target to 21
- [x] Verify the project compiles: `./mvnw compile`

## Phase 2: Configuration (application.properties)

- [x] Map Spring properties to Quarkus equivalents:
  ```
  # Spring                                    → Quarkus
  server.port=8081                            → quarkus.http.port=8081
  spring.datasource.url=jdbc:mysql://...      → quarkus.datasource.jdbc.url=jdbc:mysql://...
  spring.datasource.username=root             → quarkus.datasource.username=root
  spring.datasource.password=root             → quarkus.datasource.password=root
  spring.datasource.driver-class-name=...     → (remove, inferred from quarkus.datasource.db-kind)
  spring.jpa.hibernate.ddl-auto=update        → quarkus.hibernate-orm.schema-management.strategy=update
  spring.jpa.show-sql=true                    → quarkus.hibernate-orm.log.sql=true
  spring.jpa.properties.hibernate.dialect=... → (remove, inferred from db-kind)
  ```
- [x] Add `quarkus.datasource.db-kind=mysql`

## Phase 3: Entity Layer (Task.java)

- [x] Change `Task` to extend `PanacheEntity`
- [x] Remove manual `@Id` and `@GeneratedValue` fields (provided by `PanacheEntity`)
- [x] Convert private fields + getters/setters to public fields (Panache convention)
- [x] Keep `@Entity` and `@Table(name = "tasks")` annotations
- [x] Remove `@DateTimeFormat` (Spring-specific; use Qute formatting in templates)
- [x] Verify entity compiles

## Phase 4: Repository Layer

- [x] Replace `TaskRepository extends JpaRepository<Task, Long>` with:
  ```java
  @ApplicationScoped
  public class TaskRepository implements PanacheRepository<Task> {
  }
  ```
- [x] Add pagination method using Panache's `PanacheQuery` + `Page`:
  ```java
  public PanacheQuery<Task> findAllPaged(int pageNo, int pageSize) {
      return findAll().page(Page.of(pageNo - 1, pageSize));
  }
  ```

## Phase 5: Service Layer Simplification

- [x] Remove `TaskService.java` interface (unnecessary indirection)
- [x] Remove `TaskServiceImpl.java`
- [x] Move business logic directly into the repository or the resource class
  - `addTask(task)` → `taskRepository.persist(task)` (called from resource)
  - `deleteTask(id)` → `taskRepository.deleteById(id)` (called from resource)
  - `getAllTasks()` → `taskRepository.listAll()` (called from resource)
  - `getAllTasksPage(pageNo, pageSize)` → `taskRepository.findAllPaged(...)` (on repository)

## Phase 6: Controller → JAX-RS Resource (TaskController → TaskResource)

- [x] Rename `TaskController.java` to `TaskResource.java`
- [x] Replace Spring annotations with JAX-RS + Qute equivalents:
  ```
  @Controller             → @Path("/")
  @GetMapping("/home")    → @GET @Path("/home")
  @PostMapping("/home")   → @POST @Path("/home")
  @DeleteMapping("/{id}") → @DELETE @Path("/home/{id}")
  @ResponseBody           → (default in JAX-RS)
  @PathVariable           → @RestPath
  @RequestBody            → (auto-deserialized by Quarkus REST)
  @Autowired              → @Inject
  ```
- [x] Replace Spring `Model` with Qute `TemplateInstance`
- [x] Implement redirect for `/` → `/home` using `@GET @Path("/")` returning `Response.seeOther(...)`
- [x] Implement pagination using Panache's `PanacheQuery`
- [x] Add `@Transactional` to mutating endpoints (`POST`, `DELETE`)
- [x] Return `TemplateInstance` for HTML endpoints, JSON for REST endpoints
- [x] Added `TemplateDateExtensions.java` for `LocalDate` formatting in Qute

## Phase 7: Templates (Thymeleaf → Qute)

- [x] Rewrite `src/main/resources/templates/home.html` as a Qute template
- [x] Convert Thymeleaf syntax to Qute syntax:
  ```
  Thymeleaf                                  → Qute
  th:each="task : ${tasks}"                  → {#for task in tasks}...{/for}
  th:text="${task.title}"                     → {task.title}
  th:if="${condition}"                        → {#if condition}...{/if}
  th:unless="${condition}"                    → {#if !condition}...{/if}
  th:href="@{'/home/' + ${pageNo}}"          → href="/home/{pageNo}"
  th:attr="data-task-id=${task.id}"          → data-task-id="{task.id}"
  th:field="${task.title}"                    → name="title" value="{task.title}"
  th:action="@{/home}"                       → action="/home"
  th:object="${task}"                         → (remove, not needed)
  th:src="@{/js/home.js}"                    → src="/js/home.js"
  ${#temporals.format(task.dueDate, '...')}  → {task.dueDate.format('dd MMMM yyyy')} via @TemplateExtension
  ${#numbers.sequence(1, totalPages)}         → {#for i in totalPages} with {i_count}
  [[${i}]]                                   → {i_count}
  ```
- [x] Created `TemplateDateExtensions.java` with `@TemplateExtension` for date formatting
- [x] Convert `error.html` to Qute syntax
- [x] Move static files to `src/main/resources/META-INF/resources/js/home.js`

## Phase 8: Static Assets & JavaScript

- [x] Move `src/main/resources/static/` → `src/main/resources/META-INF/resources/`
- [x] Update `home.js`:
  - [x] Remove CSRF token handling (Quarkus does not use CSRF tokens by default)
  - [x] Verify AJAX endpoint URLs match the new JAX-RS paths
  - [x] Update `Content-Type` headers if needed

## Phase 9: Remove Spring Boot Main Class

- [x] Delete `AppApplication.java` (Quarkus does not need a main class; it generates one)

## Phase 10: Testing

- [x] Rewrite `AppApplicationTests.java` using `@QuarkusTest`:
  ```java
  @QuarkusTest
  class AppApplicationTests {
      @Test
      void homePageLoads() {
          given().when().get("/home").then().statusCode(200);
      }
  }
  ```
- [x] Add `quarkus-junit` and `rest-assured` test dependencies

## Phase 11: Verification & Cleanup

- [x] Verify project compiles: `./mvnw compile` — **BUILD SUCCESS**
- [ ] Verify project starts in dev mode: `./mvnw quarkus:dev`
- [ ] Test page rendering at `http://localhost:8081/home`
- [ ] Test task creation via the form (POST `/home`)
- [ ] Test task deletion via the delete button (DELETE `/home/{id}`)
- [ ] Test pagination with > 6 tasks
- [ ] Test error page rendering
- [x] Remove any leftover Spring imports from all Java files
- [ ] Remove the `rewrite.yml` file if no longer needed
- [ ] Verify no Spring dependencies remain in the dependency tree: `./mvnw dependency:tree`

---

## Dependency Mapping Quick Reference

| Spring Boot | Quarkus |
|---|---|
| `spring-boot-starter-parent` (parent) | `io.quarkus.platform:quarkus-bom:3.33.1` (BOM) |
| `spring-boot-starter-web` | `io.quarkus:quarkus-rest` + `io.quarkus:quarkus-rest-jackson` |
| `spring-boot-starter-data-jpa` | `io.quarkus:quarkus-hibernate-orm-panache` |
| `spring-boot-starter-thymeleaf` | `io.quarkus:quarkus-rest-qute` |
| `mysql-connector-j` | `io.quarkus:quarkus-jdbc-mysql` |
| `spring-boot-starter-test` | `io.quarkus:quarkus-junit` + `io.rest-assured:rest-assured` |
| `spring-boot-devtools` | Built-in (`./mvnw quarkus:dev`) |
| `spring-boot-maven-plugin` | `io.quarkus:quarkus-maven-plugin` |

## Annotation Mapping Quick Reference

| Spring | Quarkus / Jakarta |
|---|---|
| `@SpringBootApplication` | Not needed |
| `@Controller` | `@Path("/")` on resource class |
| `@GetMapping("/path")` | `@GET @Path("/path")` |
| `@PostMapping("/path")` | `@POST @Path("/path")` |
| `@DeleteMapping("/path")` | `@DELETE @Path("/path")` |
| `@ResponseBody` | Default behavior in JAX-RS |
| `@RequestBody` | Automatic deserialization |
| `@PathVariable` | `@RestPath` or `@PathParam` |
| `@Autowired` | `@Inject` |
| `@Service` | `@ApplicationScoped` |
| `@Repository` | `@ApplicationScoped` (with `PanacheRepository`) |
| `Model` (Spring MVC) | `TemplateInstance` (Qute) |

## Configuration Mapping Quick Reference

| Spring Boot property | Quarkus property |
|---|---|
| `server.port` | `quarkus.http.port` |
| `spring.datasource.url` | `quarkus.datasource.jdbc.url` |
| `spring.datasource.username` | `quarkus.datasource.username` |
| `spring.datasource.password` | `quarkus.datasource.password` |
| `spring.datasource.driver-class-name` | `quarkus.datasource.db-kind` (driver inferred) |
| `spring.jpa.hibernate.ddl-auto` | `quarkus.hibernate-orm.schema-management.strategy` |
| `spring.jpa.show-sql` | `quarkus.hibernate-orm.log.sql` |
| `spring.jpa.properties.hibernate.dialect` | Inferred from `db-kind` (remove) |
