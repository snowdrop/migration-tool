[![Build](https://img.shields.io/github/actions/workflow/status/snowdrop/migration-tool/mvn-build.yml?branch=main&logo=GitHub&style=for-the-badge&label=Build)](https://github.com/snowdrop/migration-tool/actions/workflows/mvn-build.yml)
[![E2E Tests](https://img.shields.io/github/actions/workflow/status/snowdrop/migration-tool/e2e-tests.yml?branch=main&logo=GitHub&style=for-the-badge&label=E2E)](https://github.com/snowdrop/migration-tool/actions/workflows/e2e-tests.yml)
[![License](https://img.shields.io/github/license/snowdrop/migration-tool?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)

# mtool - Migration Tool

**A controlled, multi-provider framework for end-to-end Java application migration.**

mtool orchestrates the entire migration lifecycle -- from analysis to transformation -- using YAML-defined rules with ordered instructions that can be executed by different providers: OpenRewrite recipes, AI (Claude), or manual guidance. Unlike running OpenRewrite directly, mtool gives you a controlled pipeline where rules execute in the right order, conditions are verified before transformations begin, and multiple providers can collaborate on the same migration plan.

---

## Why mtool? The Problem with Raw OpenRewrite

OpenRewrite is a powerful code transformation engine. But when you try to use it for a full application migration (e.g., Spring Boot to Quarkus), you quickly hit real-world friction:

| Challenge | Raw OpenRewrite | mtool |
|-----------|----------------|-------|
| **Execution order** | Recipes run in recipe-pipeline order, but cross-recipe dependencies (e.g., "remove deps before changing annotations") require careful manual orchestration | Rules have an explicit `order` field. Dependencies are removed before annotations change, properties convert after code. The pipeline is predictable. |
| **Eligibility checks** | No built-in way to check if a project *should* be migrated before attempting it | `precondition` field gates the entire analysis -- if the project doesn't match, the process stops early |
| **Condition language** | Recipe preconditions use Java visitors -- powerful but verbose | Human-readable ANTLR query language: `java.annotation is 'SpringBootApplication'`, `pom.dependency is (gavs='...')` with `AND`/`OR` support |
| **Multiple transformation strategies** | OpenRewrite only. If a recipe doesn't exist, you're stuck. | Three providers in one pipeline: OpenRewrite for deterministic transforms, AI (Claude) for complex reasoning, manual TODOs for human judgment |
| **Debugging & testing** | Hard to isolate which recipe caused a failure in a composite run | Each rule is atomic. Analyze first (dry inspection), then transform. Each step produces a traceable migration plan in JSON |
| **Migration plan as artifact** | No intermediate artifact -- recipes run or they don't | The analysis phase generates a JSON migration plan with matched rules, instructions, and provider-specific tasks. Review before you transform. |

In short: OpenRewrite is an excellent *engine*. mtool is the *cockpit* that makes it safe, predictable, and extensible for real-world migrations.

---

## How It Works

```
                                   YAML Rules (cookbook/)
                                         |
                    +--------------------+--------------------+
                    |                                         |
              1. ANALYZE                                2. TRANSFORM
                    |                                         |
          Parse conditions (ANTLR)                   Load migration plan (JSON)
          Dispatch to scanners                       Sort by rule order
          (OpenRewrite, Maven, JDT-LS)               Select provider
          Generate migration plan                         |
                    |                          +----------+----------+
                    v                          |          |          |
           Migration Plan (JSON)          OpenRewrite    AI      Manual
           - Matched rules                (recipes)   (Claude)  (TODOs)
           - Provider instructions             |          |          |
           - Execution order                   v          v          v
                                           Migrated Application
```

### Phase 1: Analyze

Scans your project against a set of YAML rules. Each rule's condition is parsed through an ANTLR grammar and dispatched to the appropriate scanner (OpenRewrite for Java/properties, Maven for pom.xml, JDT-LS for advanced Java analysis). Produces a JSON migration plan.

### Phase 2: Transform

Reads the migration plan and executes each rule's instructions in order, using the selected provider:

- **OpenRewrite**: Runs recipes via the Maven plugin. Deterministic, reproducible, supports dry-run mode with patch output.
- **AI (Claude)**: Sends each task to Claude via Langchain4j with file system access. Claude reads the actual project files, reasons about the changes, and writes the transformed code. Best for complex refactors where no recipe exists.
- **Manual**: Prints a structured TODO list for each rule. Useful for changes that require human judgment or review.

---

## Quick Start

### Requirements

- Java 21
- Maven 3.9+

### Install via JBang

```bash
jbang app install mtool@snowdrop/migration-tool
```

### Or build from source

```bash
git clone https://github.com/snowdrop/migration-tool.git
cd migration-tool
mvn clean install -DskipTests
```

### Basic usage

```bash
# Analyze a project
mtool analyze ./my-spring-app -r ./cookbook/rules/quarkus-spring --scanner openrewrite

# Review the generated migration plan
cat ./my-spring-app/analysing-*-report_*.json

# Transform using OpenRewrite
mtool transform ./my-spring-app -p openrewrite

# Or transform using AI (Claude)
mtool transform ./my-spring-app -p ai

# Or get a manual TODO list
mtool transform ./my-spring-app -p manual
```

---

## Rule Format

Rules are the core unit of work. Each rule defines *what to look for* and *what each provider should do about it*:

```yaml
- ruleID: 010-replace-bom
  category: mandatory
  description: Replace Spring Boot parent with Quarkus BOM
  order: 1                    # Execution sequence across the pipeline
  labels:
    - konveyor.io/source=springboot
    - konveyor.io/target=quarkus

  when:
    # Optional: gate the entire analysis
    precondition: pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-parent:3.5.3')
    # What to match in the source code
    condition: pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-parent')

  instructions:
    openrewrite:
      - name: Replace Spring Boot parent with Quarkus BOM
        recipeList:
          - org.openrewrite.maven.RemoveManagedDependency:
              groupId: org.springframework.boot
              artifactId: spring-boot-starter-parent
          - org.openrewrite.maven.AddManagedDependency:
              groupId: io.quarkus.platform
              artifactId: quarkus-bom
              version: 3.26.4
              type: pom
              scope: import
        gav:
          - org.openrewrite:rewrite-maven:8.71.0
    ai:
      - tasks:
          - "Read the pom.xml file. Remove the Spring Boot parent section. Add the Quarkus BOM..."
    manual:
      - todo: "In pom.xml, replace the Spring Boot parent with the Quarkus BOM..."
```

### Query Language

The condition syntax is designed to be readable and expressive:

```yaml
# Simple match
condition: java.annotation is 'org.springframework.stereotype.Controller'

# Compound conditions
condition: |
  pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-web') AND
  java.annotation is 'org.springframework.stereotype.Controller'

# OR conditions
condition: |
  java.annotation is 'org.springframework.stereotype.Controller' OR
  java.annotation is 'org.springframework.web.bind.annotation.GetMapping'

# Property matching
condition: properties.key is 'quarkus.resteasy.*'

# File search
condition: source.file is 'import.sql'
```

Supported file types: `java`, `pom`, `text`, `property`, `yaml`, `json`

---

## Project Structure

```
migration-tool/
|-- model/                  # Data models: Rule, MigrationTask, Query, Config
|-- parser/                 # ANTLR grammar and query language parser
|-- scanner/                # Pluggable scanners (OpenRewrite, Maven, JDT-LS, FileSearch)
|-- openrewrite-recipes/    # 19 custom OpenRewrite recipes (Spring, Maven, Java)
|-- migration-cli/          # Quarkus CLI app (analyze + transform commands)
|-- tests/                  # Integration tests
|-- cookbook/rules/          # YAML rule definitions by migration scenario
|-- applications/           # Sample apps for testing and demos
```

### Scanner Architecture

Scanners are resolved via Java SPI. Each scanner declares which query types it supports:

| Scanner | Supports | Used for |
|---------|----------|----------|
| `OpenRewriteQueryScanner` | `java.annotation`, `properties.key`, `source.file` | Java code analysis, property matching |
| `MavenQueryScanner` | `pom.dependency` | Maven dependency detection |
| `JdtlsQueryScanner` | `java.annotation`, `java.referenced` | Advanced Java analysis via Language Server |
| `FileSearchQueryScanner` | `text`, `json`, `yaml` | Generic file content search |

The `ScannerSpiRegistry` selects the best scanner for each query, falling back through alternatives if the preferred scanner doesn't support the query type.

---

## Migration Scenarios

### Scenario 1: Spring Boot to Quarkus (OpenRewrite)

The flagship migration scenario. 9 rules covering the full lifecycle:

| Order | Rule | What it does |
|-------|------|-------------|
| 1 | `010-replace-bom` | Replace Spring Boot parent with Quarkus BOM, add quarkus-arc/core deps |
| 1 | `011-replace-dependency-jpa` | Swap spring-boot-starter-data-jpa for quarkus-hibernate-orm-panache + JDBC driver |
| 1 | `012-replace-dependency-rest` | Swap spring-boot-starter-web for quarkus-rest + quarkus-rest-jackson |
| 3 | `013-springboot-to-quarkus-main` | Replace @SpringBootApplication with @QuarkusMain, SpringApplication.run() with Quarkus.run() |
| 4 | `020-rest-annotations` | Convert @Controller + @GetMapping to @Path + @GET (JAX-RS) |
| 5 | `030-add-sql` | Create import.sql with seed data for Quarkus Hibernate |
| 5 | `031-replace-properties` | Convert application.properties from Spring to Quarkus format |
| 6 | `041-rest-get-tasks` | Refactor REST endpoint to return entity list directly |
| 6 | `042-redirect` | Convert Spring redirect to Quarkus RestResponse redirect |

```bash
# Analyze
mtool analyze ./applications/spring-boot-todo-app \
  -r ./cookbook/rules/quarkus-spring \
  --scanner openrewrite

# Transform
mtool transform ./applications/spring-boot-todo-app -p openrewrite

# Verify
cd applications/spring-boot-todo-app && mvn compile
```

### Scenario 2: Spring Boot to Quarkus (AI with Claude)

Same migration, but using Claude as the transformation provider. The AI reads each file, understands the context, and writes the transformed code:

```bash
# Configure Claude API access
export QUARKUS_LANGCHAIN4J_ANTHROPIC_API_KEY=<your-key>
export QUARKUS_LANGCHAIN4J_ANTHROPIC_BASE_URL=<api-server>
export QUARKUS_LANGCHAIN4J_ANTHROPIC_CHAT_MODEL_MODEL_NAME=premium

# Same analysis step
mtool analyze ./applications/spring-boot-todo-app \
  -r ./cookbook/rules/quarkus-spring \
  --scanner openrewrite

# Transform with AI
mtool transform ./applications/spring-boot-todo-app -p ai
```

Each rule's AI instructions are designed to give Claude precise, file-level tasks with enough context to produce correct transformations. The AI provider injects the project path so Claude knows where to find and write files.

### Scenario 3: Quarkus RESTEasy Classic to Reactive

Demonstrates that mtool isn't limited to cross-framework migrations. This scenario upgrades a Quarkus application from RESTEasy Classic to RESTEasy Reactive:

| Order | Rule | What it does |
|-------|------|-------------|
| 1 | `010-replace-resteasy-deps` | Swap `quarkus-resteasy` for `quarkus-rest`, upgrade Quarkus to 3.26.4 |
| 2 | `020-update-jaxrs-response` | Replace `jakarta.ws.rs.core.Response` with type-safe `RestResponse<T>` |
| 3 | `030-update-properties` | Rename `quarkus.resteasy.*` properties to `quarkus.rest.*` |

```bash
# Analyze with quarkus source/target labels
mtool analyze ./applications/quarkus-resteasy-classic-app \
  -r ./cookbook/rules/quarkus-resteasy-reactive \
  --source quarkus --target quarkus \
  --scanner openrewrite

# Transform
mtool transform ./applications/quarkus-resteasy-classic-app -p openrewrite

# Review changes
cd applications/quarkus-resteasy-classic-app && git diff
```

---

## AI Provider Configuration

To use Claude as the transformation engine, set these environment variables (or add them to a `.env` file):

```properties
QUARKUS_LANGCHAIN4J_ANTHROPIC_API_KEY=<your-api-key>
QUARKUS_LANGCHAIN4J_ANTHROPIC_BASE_URL=<api-server-url>
QUARKUS_LANGCHAIN4J_ANTHROPIC_CHAT_MODEL_MODEL_NAME=premium
QUARKUS_LANGCHAIN4J_ANTHROPIC_TIMEOUT=60
```

The AI provider gives Claude access to a `FileSystemTool` with `readFile` and `writeFile` operations, scoped to the project being migrated. Each task from the rule's `instructions.ai.tasks` list is sent as a separate chat message with the project path as context.

---

## CLI Reference

### `mtool analyze`

```
Usage: mtool analyze [-v] [-o=<output>] [-r=<rulesPath>] [-s=<source>]
                     [--scanner=<scanner>] [-t=<target>] <appPath>

Arguments:
  <appPath>               Path to the Java project to analyze

Options:
  -r, --rules=<path>      Path to rules directory
  -s, --source=<source>   Source technology label (e.g., springboot, quarkus)
  -t, --target=<target>   Target technology label (e.g., quarkus)
  --scanner=<scanner>     Scanner engine: openrewrite (default), jdtls
  -o, --output=<format>   Output format: json, csv, html
  -v, --verbose           Enable verbose output
```

### `mtool transform`

```
Usage: mtool transform [-dv] [-p=<provider>] <appPath>

Arguments:
  <appPath>               Path to the Java project to transform

Options:
  -p, --provider=<type>   Provider: openrewrite (default), ai, manual
  -d, --dry-run           Preview changes without applying (OpenRewrite only)
  -v, --verbose           Enable verbose output
```

---

## Development

```bash
# Build everything
mvn clean install -DskipTests

# Run tests
mvn test -pl tests

# Run a specific test
mvn test -pl tests -Dtest=RulesTest#testMultipleRulesInSequence

# Dev mode (with hot reload)
mvn -pl migration-cli quarkus:dev -Dquarkus.args="analyze ../applications/spring-boot-todo-app"

# Native binary
mvn clean package -Pnative
```

### Writing Custom Rules

1. Create a YAML file in `cookbook/rules/<your-scenario>/`
2. Define the condition using the query language
3. Add instructions for one or more providers
4. Set the `order` field to control execution sequence

### Writing Custom OpenRewrite Recipes

Custom recipes live in `openrewrite-recipes/`. The module is automatically included as a dependency when referenced in rule GAV coordinates:

```yaml
gav:
  - dev.snowdrop.mtool:openrewrite-recipes:1.0.5-SNAPSHOT
```

---

## Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Quarkus | 3.30.6 | CLI framework (Picocli), CDI, AI integration |
| OpenRewrite BOM | 3.22.0 | Code transformation recipes |
| rewrite-client | 0.2.2 | In-process recipe execution (analysis phase) |
| Langchain4j | (managed by Quarkus) | Claude AI integration |
| Eclipse LSP4J | 0.24.0 | JDT Language Server communication |
| ANTLR | 4.x | Query language parser generation |

---

## License

[Apache License 2.0](LICENSE)
