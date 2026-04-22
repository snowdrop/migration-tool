---
name: migrate-spring-to-quarkus-old
description: Migrate Spring Boot applications to Quarkus. Use when the user wants to migrate, convert, 
  or port a Spring Boot app to Quarkus, or asks about Spring-to-Quarkus migration steps. 
  Supports both Spring compatibility extensions and native Quarkus migration paths.
---

# Migrate Spring Boot to Quarkus

Guide the user through a structured, phased migration of a Spring Boot application to Quarkus.

## Critical Rules

- **Never delete code you cannot migrate.** If you cannot fully migrate a piece of code, leave the original in place with a `// TODO: Migration required — <reason>` comment explaining what needs to change and why. This applies to:
  - Methods, classes, or annotations you don't know how to convert
  - Spring-specific patterns without a clear Quarkus equivalent
  - Configuration or wiring code whose purpose is unclear
  If you must remove code (e.g., a Spring-only base class), document what was removed and why in a `// REMOVED:` comment at the same location.
- **Don't break the build.** Run `mvn clean compile -DskipTests` after each phase. Never move to the next phase with a broken build.
- **Document every decision.** When choosing between migration approaches, explain the trade-off to the user.
- **No silent changes.** Every file modification must be intentional and traceable. If a check fails after a phase, diagnose and fix — don't skip the check or delete the failing code.

## Reference Files

Load the relevant reference file before each phase:

| Reference | Use during |
|---|---|
| [references/dependency-map.md](references/dependency-map.md) | Phase 1-2: Build system and dependency mapping |
| [references/annotation-map.md](references/annotation-map.md) | Phase 3-4: Code migration (DI, REST, Data, Security, Test, Lifecycle) |
| [references/config-map.md](references/config-map.md) | Phase 5: Configuration property migration |


## Pre-migration setup

Before starting the migration, explain the following workflow to the user and ask for confirmation before proceeding:

> **Migration workflow:** Each migration run is isolated in its own branch (`migration/run-01`, `migration/run-02`, ...) created from `main`. The branch will contain a single    
commit with all changes plus a migration report. A draft PR against `main` will be created for review — it is never merged, it serves as a permanent diff and discussion record.
Before running, the skill will self-update from its source repository to ensure you are always using the latest version.

If the user confirms, proceed with the steps below. If the user wants to adjust anything (branch name, remote, PR format), adapt accordingly.


### 1. Self-update: fetch and install the latest version of this skill

This skill's source of truth is the repository `github.com/aureamunoz/migration-tool`, branch `experiment-skills-4-migrating`, folder `.claude/skills/migrate-spring-to-quarkus`.

Fetch the latest version and overwrite the local copy:

```bash
SKILL_REPO_DIR=$(mktemp -d)
git clone --depth 1 --branch experiment-skills-4-migrating https://github.com/aureamunoz/migration-tool.git "$SKILL_REPO_DIR"
SKILL_SHA=$(git -C "$SKILL_REPO_DIR" rev-parse HEAD)

# Overwrite the local skill with the latest version
cp -R "$SKILL_REPO_DIR/.claude/skills/migrate-spring-to-quarkus/" .claude/skills/migrate-spring-to-quarkus/

rm -rf "$SKILL_REPO_DIR"
```

Store `$SKILL_SHA` — it is needed for the commit message later.

After updating, **re-read the skill file** (`.claude/skills/migrate-spring-to-quarkus/skill.md` or equivalent) and follow those instructions for the rest of the migration. If the fetched version differs from what is currently loaded, the fetched version takes precedence.

### 2. Create the migration branch

Determine the next run number from existing branches:

```bash
git branch -a --list '*migration/run-*' | sort -t- -k3 -n | tail -1
```

Then create the next numbered branch from `main`:

```bash
git checkout main
git checkout -b migration/run-XX
```

Where `XX` is the next sequential number (zero-padded to two digits).

## Post-migration steps

After the migration is complete and verified:

### 1. Commit

Create a single commit with all migration changes plus `migration-report.md` at the repo root:

```
Migrate Spring Boot to Quarkus

Migrated by Claude, using <SKILL_SHA> skill
```

### 2. Push and create draft PR

```bash
git push auri migration/run-XX
gh pr create --draft --title "migration/run-XX: Spring Boot → Quarkus" --body "$(cat migration-report.md)"
```

The draft PR is a permanent record — never merge it. `main` always keeps the original code. Use labels to categorize runs (e.g., `strategy:native`, `strategy:spring-compat`, `skill-version:vN`).

## Step 1: Analyze the Spring Boot Application

Scan the application to understand what needs to migrate:

- **Build system**: Read `pom.xml`, identify Spring Boot version, list starters and plugins
- **Java code**: Search for Spring annotations — see [annotation-map.md](references/annotation-map.md) for the full list to check
- **Configuration**: Read `application.properties`/`application.yml`, check for profiles
- **UI / View layer**: Check for Thymeleaf/JSP templates, static resources, Model+View patterns

Present a summary table with area, findings, and migration complexity. Ask the user to confirm before proceeding.

## Step 2: Plan the Migration

Propose a phased plan. Each phase maps to a rule execution order:

| Phase | Order | What |
|-------|-------|------|
| 1. Build system | 1 | Replace Spring Boot parent with Quarkus BOM, swap plugins, add core deps |
| 2. Dependencies | 2-3 | Replace Spring starters with Quarkus equivalents — see [dependency-map.md](references/dependency-map.md) |
| 3. Data layer | 4 | Entities (Panache), repositories, service layer simplification |
| 4. Controllers | 5 | Annotations, `Model` → Qute templates, `redirect:` → `Response.seeOther()` |
| 5. Configuration | 6 | Map Spring properties to Quarkus equivalents — see [config-map.md](references/config-map.md) |
| 6. Templates & assets | 7 | Thymeleaf → Qute, static resources to `META-INF/resources/`, remove CSRF |
| 7. Cleanup | 8 | Remove main class, leftover Spring imports, final verification |

Ask the user which approach they prefer:
- **Spring compat** (recommended): Use `quarkus-spring-web`, `quarkus-spring-data-jpa`, etc. Minimal code changes.
- **Native Quarkus**: Replace all Spring annotations with JAX-RS/CDI. More work, full Quarkus experience.

## Step 3: Execute the Migration

Execute each phase in order:

1. **Prefer mtool when available:**
   ```bash
   mtool analyze <app-path> -r cookbook/rules/quarkus-spring --scanner openrewrite
   mtool transform <app-path> -p openrewrite
   ```

2. **Mechanical changes** (deps, properties, type renames): Make directly using the mapping tables.

3. **Complex changes** (view layer, REST clients, security): Explain before/after, get user confirmation. If you cannot migrate something, add a TODO comment — never silently delete.

4. **After each phase**, verify: `mvn clean compile -DskipTests`

### Phase 1 Reference: Build System (pom.xml)

When migrating `pom.xml`, use these XML blocks as reference. Replace the Spring Boot parent and plugin with the Quarkus equivalents:

**Remove** the Spring Boot parent:
```xml
<!-- DELETE this -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>...</version>
</parent>
```

**Add** Quarkus BOM in `<dependencyManagement>`:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Add** the Quarkus Maven plugin and update compiler/surefire:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
            <version>${quarkus.platform.version}</version>
            <extensions>true</extensions>
            <executions>
                <execution>
                    <goals>
                        <goal>build</goal>
                        <goal>generate-code</goal>
                        <goal>generate-code-tests</goal>
                        <goal>native-image-agent</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${compiler-plugin.version}</version>
            <configuration>
                <parameters>true</parameters>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <configuration>
                <systemPropertyVariables>
                    <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                </systemPropertyVariables>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Define `quarkus.platform.version` as a property (use the latest Quarkus release). Do NOT hardcode the version — check https://quarkus.io for the current release.

### Phase 3 Reference: JPA / Data Layer (Panache)

When using the **native Quarkus** migration path (not Spring compat), convert Spring Data repositories to Panache. Two patterns are available:

**Active Record pattern** (entity contains query methods):

```java
// BEFORE: Spring Data
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCompleted(boolean completed);
}

// AFTER: Panache Active Record
@Entity
public class Todo extends PanacheEntity {
    public String title;
    public boolean completed;

    public static List<Todo> findByCompleted(boolean completed) {
        return list("completed", completed);
    }
}
// Usage: Todo.listAll(), Todo.findById(id), Todo.findByCompleted(true)
// Note: 'id' field is provided by PanacheEntity — remove @Id from entity
```

**Repository pattern** (separate repository class):

```java
// BEFORE: Spring Data
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCompleted(boolean completed);
}

// AFTER: Panache Repository
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    public List<Todo> findByCompleted(boolean completed) {
        return list("completed", completed);
    }
}
// Usage: todoRepository.listAll(), todoRepository.findById(id)
```

**Pagination with PanacheQuery:**

```java
// BEFORE: Spring Data
Page<Todo> findByCompleted(boolean completed, Pageable pageable);

// AFTER: Panache Repository
public List<Todo> findByCompleted(boolean completed, int page, int size) {
    return find("completed", completed).page(Page.of(page, size)).list();
}
// For Page-like metadata, use PanacheQuery:
PanacheQuery<Todo> query = find("completed", completed);
query.page(Page.of(page, size));
long totalCount = query.count();
List<Todo> items = query.list();
```

When the user chose **Spring compat** path, keep `JpaRepository`/`CrudRepository` interfaces — they work with `quarkus-spring-data-jpa`.

### Phase 3 Reference: Service Layer

In Spring Boot apps, services often follow the interface + implementation pattern (`TodoService` interface + `TodoServiceImpl` class). In Quarkus, this indirection is usually unnecessary. For the **native Quarkus** path:

```java
// BEFORE: Spring — interface + impl
public interface TodoService {
    List<Todo> findAll();
    Todo save(Todo todo);
}

@Service
public class TodoServiceImpl implements TodoService {
    @Autowired
    private TodoRepository repository;

    @Override
    public List<Todo> findAll() { return repository.findAll(); }

    @Override
    public Todo save(Todo todo) { return repository.save(todo); }
}

// AFTER: Quarkus — single class with @ApplicationScoped
@ApplicationScoped
public class TodoService {
    @Inject
    TodoRepository repository;

    public List<Todo> findAll() { return repository.listAll(); }
    public Todo save(Todo todo) { repository.persist(todo); return todo; }
}
```

**Decision guide:**
- If the service only delegates to the repository → consider eliminating it and injecting the repository directly in the resource
- If the service has real business logic → keep it as a single `@ApplicationScoped` class, remove the interface
- If the interface is used for testing/mocking → Quarkus `@InjectMock` works on concrete classes, no interface needed

For **Spring compat** path, `@Service` is supported by `quarkus-spring-di` — no changes needed beyond ensuring the class has a scope.

### Phase 5 Reference: Thymeleaf → Qute Templates

When migrating templates from Thymeleaf to Qute, use this syntax conversion table:

| Thymeleaf | Qute | Notes |
|---|---|---|
| `th:text="${name}"` | `{name}` | Direct expression |
| `th:utext="${html}"` | `{html.raw}` | Unescaped HTML output |
| `th:each="item : ${items}"` | `{#for item in items}...{/for}` | Loop |
| `th:if="${condition}"` | `{#if condition}...{/if}` | Conditional |
| `th:unless="${condition}"` | `{#if !condition}...{/if}` | Negated conditional |
| `th:href="@{/path/{id}(id=${item.id})}"` | `href="/path/{item.id}"` | URL with path param |
| `th:action="@{/submit}"` | `action="/submit"` | Form action |
| `th:value="${value}"` | `value="{value}"` | Input value |
| `th:class="${active ? 'on' : 'off'}"` | `class="{active ? 'on' : 'off'}"` | Conditional class |
| `th:fragment="name"` | `{#include name /}` | Template fragment/include |

**Controller → Qute type-safe templates:**

```java
// BEFORE: Spring MVC + Thymeleaf
@Controller
public class TodoController {
    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";  // resolves to templates/todos.html
    }
}

// AFTER: Quarkus + Qute
@Path("/todos")
@ApplicationScoped
public class TodoResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance todos(List<Todo> todos);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.todos(todoService.findAll());
    }
}
```

File rename: `templates/todos.html` → `templates/TodoResource/todos.html` (must match the enclosing class name when using `@CheckedTemplate`).

**`Model.addAttribute()` → `Template.data()`:**

```java
// BEFORE: Spring MVC — Model as parameter
@GetMapping("/todos/{id}")
public String detail(@PathVariable Long id, Model model) {
    model.addAttribute("todo", todoService.findById(id));
    model.addAttribute("categories", categoryService.findAll());
    return "todo-detail";
}

// AFTER: Quarkus + Qute — type-safe template data
@GET
@Path("/{id}")
@Produces(MediaType.TEXT_HTML)
public TemplateInstance detail(@PathParam("id") Long id) {
    return Templates.todoDetail(todoService.findById(id), categoryService.findAll());
}

@CheckedTemplate
public static class Templates {
    public static native TemplateInstance todoDetail(Todo todo, List<Category> categories);
}
```

**`return "redirect:..."` → `Response.seeOther()`:**

```java
// BEFORE: Spring MVC
@PostMapping("/todos")
public String create(@ModelAttribute Todo todo) {
    todoService.save(todo);
    return "redirect:/todos";
}

// AFTER: Quarkus + JAX-RS
@POST
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public Response create(@BeanParam Todo todo) {
    todoService.save(todo);
    return Response.seeOther(URI.create("/todos")).build();
}
```

### Phase 5 Reference: Static Assets & CSRF

Move static resources from Spring Boot's default location to Quarkus:

```
# BEFORE (Spring Boot)
src/main/resources/static/css/style.css
src/main/resources/static/js/app.js

# AFTER (Quarkus)
src/main/resources/META-INF/resources/css/style.css
src/main/resources/META-INF/resources/js/app.js
```

**Remove Spring CSRF tokens** from JavaScript/HTML — Quarkus does not use Spring Security's CSRF mechanism:

```javascript
// DELETE these from JS files:
const token = document.querySelector('meta[name="_csrf"]').content;
const header = document.querySelector('meta[name="_csrf_header"]').content;
headers[header] = token;
```

```html
<!-- DELETE these from HTML templates: -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
```

If the app needs CSRF protection in Quarkus, use `quarkus-csrf-reactive` (form-based) or configure it via `quarkus.http.csrf`.

### Phase 5 Reference: Main Class Removal

Quarkus does not need a `@SpringBootApplication` main class — it auto-generates one.

```java
// DELETE this entire file (e.g., Application.java / MyApp.java):
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

If the main class contains `@Bean` definitions or `CommandLineRunner` logic, migrate those first:
- `@Bean` methods → move to a `@ApplicationScoped` configuration class with `@Produces`
- `CommandLineRunner` / `ApplicationRunner` → convert to `void onStart(@Observes StartupEvent event)`
- Then delete the main class file

## Step 4: Verify the Migration

Run each check in order. A check fails = stop and fix before continuing.

| # | Check | Command | Pass criteria |
|---|-------|---------|---------------|
| 1 | **Builds** | `mvn clean package -DskipTests` | Exit code 0, no compilation errors |
| 2 | **No Spring deps** | Search `pom.xml` for `org.springframework` | Zero Spring dependencies remaining (except Spring compat extensions if using that strategy) |
| 3 | **Has Quarkus** | Search `pom.xml` for `io.quarkus` | Quarkus BOM and at least one Quarkus extension present |
| 4 | **Tests pass** | `mvn test` | All tests pass using `@QuarkusTest` |
| 5 | **Starts up** | `mvn quarkus:dev` | App starts, `curl http://localhost:8080/q/health` returns UP |
| 6 | **No leftover templates** | Search for Thymeleaf/JSP references | No remaining template engine references (unless intentionally kept with Quarkus extension) |

If any check fails, diagnose the root cause and fix it. Do not skip checks or delete failing code to make them pass.

## Step 5: Migration Review (Self-Reflection)

After completing all checks, review your own migration work. This step improves skill quality over time.

### Review checklist

Answer each question honestly:

1. **What migrated cleanly?** List patterns/areas that mapped 1:1 (e.g., "JPA entities needed no changes", "REST endpoints mapped directly to JAX-RS").
2. **What required manual judgment?** List areas where you had to make non-obvious decisions (e.g., "Replaced Spring Security config with Quarkus properties — no direct equivalent for custom filter chain").
3. **What was left as TODO?** List every `// TODO: Migration required` comment you added and why.
4. **Was any code removed?** If yes, list exactly what was removed and justify each removal. Flag any removal that might cause runtime issues.
5. **What checks failed initially?** List failures from Step 4 and what you did to fix them.
6. **What's missing from the skill references?** Note any mappings (deps, annotations, config) that were missing from the reference files and that you had to figure out.

### Migration Report

Present the review as a structured report:

```
## Migration Report: [app-name]

### Summary
- Strategy: [Full Migration / Spring Compatibility]
- Phases completed: [X/7]
- Checks passed: [X/6]

### Changes by Phase
| Phase | Files changed | Key changes |
|-------|--------------|-------------|
| 1. Build | pom.xml | Replaced Spring Boot parent with Quarkus BOM |
| ... | ... | ... |

### Validation Results
| Check | Result | Notes |
|-------|--------|-------|
| Builds | PASS/FAIL | |
| No Spring deps | PASS/FAIL | |
| Has Quarkus | PASS/FAIL | |
| Tests pass | PASS/FAIL | |
| Starts up | PASS/FAIL | |
| No leftover templates | PASS/FAIL | |

### Unmigrated Code (TODOs)
| File | Line | What | Why not migrated |
|------|------|------|-----------------|

### Removed Code
| File | What was removed | Justification |
|------|-----------------|---------------|

### Skill Improvement Suggestions
- [Any missing mappings, unclear instructions, or edge cases discovered]
```

This report helps the user understand exactly what happened and provides feedback to improve the migration skill for future runs.

## Common Pitfalls

- **Missing `@Transactional`**: Quarkus uses `jakarta.transaction.Transactional`, not Spring's
- **Bean discovery**: Quarkus uses build-time CDI; beans must have a scope annotation
- **No OSIV**: Quarkus doesn't have Open Session in View; lazy loading outside transactions will fail
- **Static resources**: Place in `src/main/resources/META-INF/resources/` (not `static/`)
- **Test port**: Quarkus tests default to port 8081. If app uses 8081, add `quarkus.http.test-port=0`
- **No component scanning**: Beans in external JARs need a Jandex index or `quarkus.index-dependency`
- **Profile handling**: Spring's `application-{profile}.properties` → Quarkus `%profile.` prefix
- **Naming strategy mismatch**: Spring Boot defaults to snake_case (`firstName` → `first_name`). Quarkus/Hibernate 6 preserves camelCase as-is. Set `quarkus.hibernate-orm.physical-naming-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy` to match Spring Boot behavior. **Also update `import.sql`/`data.sql` column names** to match.
- **JAX-RS path conflicts**: Spring allows multiple `@RestController` classes with overlapping `@RequestMapping` paths — JAX-RS does not. When migrating multiple controllers, check for duplicate `@Path` values and consolidate or disambiguate.
- **Qute strict rendering**: Qute defaults to `strict-rendering=true` — missing template variables throw exceptions, unlike Thymeleaf which outputs empty strings. Start migration with `quarkus.qute.strict-rendering=false` and `quarkus.qute.property-not-found-strategy=output-original` to find issues, then enable strict mode.
- **`@InjectMock` package change**: Since Quarkus 3.2, use `io.quarkus.test.InjectMock` (not `io.quarkus.test.junit.mockito.InjectMock`). Old package deprecated in 3.2, removed in 4.0.

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-*` extensions), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-di` | `@Primary`, `@Conditional*`, `@Profile`, `@Lazy` not processed. SpEL `#{...}` in `@Value` throws error. `@Bean` must be inside `@Configuration` class. |
| `quarkus-spring-web` | Only `@RestController` — plain `@Controller` not supported. Only one `@RestControllerAdvice` per app. `@CrossOrigin`, `@InitBinder`, `@ModelAttribute` not supported. No reactive return types (`Mono`, `Flux`). |
| `quarkus-spring-security` | Limited SpEL in `@PreAuthorize`: only `hasRole`, `hasAnyRole`, `permitAll`, `denyAll`, `isAuthenticated`, `@bean.method()`, param comparison. Cannot mix `and`/`or` operators. Cannot combine `@Secured` with `@PreAuthorize`. |
| `quarkus-spring-data-jpa` | SpEL `#{...}` in `@Query` not supported. No `Distinct` queries. Limited custom repository fragment support. |
| `quarkus-spring-cache` | Single cache name only (no arrays). `key`, `condition`, `unless`, `keyGenerator`, `cacheManager` parameters NOT supported. No `@Caching` or `@CacheConfig`. |
| `quarkus-spring-scheduled` | `fixedDelay` NOT supported (only `fixedRate`). Cannot combine `initialDelay` with `cron`. |
| `quarkus-spring-boot-properties` | `@ConstructorBinding` NOT supported (needs no-arg constructor + setters). `Map<K,V>` types NOT supported. |