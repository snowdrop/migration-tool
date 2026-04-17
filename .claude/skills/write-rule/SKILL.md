---
name: write-rule
description: Create migration rules for mtool (migration-tool). Use when the user wants to write, create, or add a new YAML migration rule for Java application migrations (Spring Boot to Quarkus, RESTEasy Classic to Reactive, etc.).
compatibility: Requires the migration-tool project with cookbook/rules/ directory structure.
metadata:
  author: snowdrop
  version: "1.0"
---

# Write Migration Rule

Help the user create well-structured YAML rules for mtool.

## Process

1. Identify the migration scenario from the user's description
2. Read the matching reference file from `references/` (see index below)
3. Ask clarifying questions if the scenario is ambiguous
4. Determine the right `when` condition using the query language
5. Propose a file path and name — **wait for user confirmation before writing**
6. Write the rule, preferring OpenRewrite for mechanical changes and AI for complex ones
7. Update the reference file's "Next available file number"

## Reference Files

Before writing a rule, read the relevant reference for existing rules, conditions, mappings, and GAV coordinates:

| Scenario | Reference | Rules directory |
|---|---|---|
| Spring Boot to Quarkus (Spring compat) | [references/quarkus-spring.md](references/quarkus-spring.md) | `cookbook/rules/quarkus-spring/` |
| Spring Boot to Native Quarkus | [references/quarkus-native.md](references/quarkus-native.md) | `cookbook/rules/quarkus/` |
| RESTEasy Classic to Reactive | [references/resteasy-reactive.md](references/resteasy-reactive.md) | `cookbook/rules/quarkus-resteasy-reactive/` |

If no reference matches, check existing rules in `cookbook/rules/` and create a new reference file.

## Output Conventions

- Propose: `cookbook/rules/<scenario>/<NNN>-<descriptive-name>.yaml`
  - Pick `<scenario>` from existing directories or suggest a new one
  - Pick `<NNN>` as the next available number from the reference file
  - Present the path to the user for confirmation before writing

- Default: **one rule per file**
- If the user includes "bundle" in their request, group related rules into a single YAML list

## Rule YAML Structure

```yaml
- category: mandatory|optional
  customVariables: []
  description: Short description of the migration
  effort: 1  # 1 = low, 2 = medium
  labels:
    - konveyor.io/source=<source-technology>
    - konveyor.io/target=<target-technology>
  links: []
  message: "User-facing message explaining the migration."
  ruleID: <NNN>-descriptive-kebab-case-id
  when:
    condition: <query-language-expression>
  order: <integer>  # Lower = runs first
  instructions:
    openrewrite:
      - name: Recipe name
        description: What this recipe does
        recipeList:
          - org.openrewrite.some.Recipe:
              param1: value1
        gav:
          - org.openrewrite:rewrite-maven:8.73.0
    ai:
      - tasks:
          - "Step-by-step instruction for AI to execute"
    manual:
      - todo: "Human-readable instruction"
```

## Query Language

**File types:** `java`, `pom`, `text`, `properties`, `yaml`, `json`

**Format:** `fileType.symbol is value` or `fileType.symbol is (key=value)`

**Operators:** `AND`, `OR`

```yaml
java.annotation is 'org.springframework.boot.autoconfigure.SpringBootApplication'
pom.dependency is (gavs='io.quarkus:quarkus-resteasy')
pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-parent:2.5.*')
properties.key is 'spring.datasource.*'
source.file is '**/*.java'
```

## Order Conventions

| Order | Purpose |
|-------|---------|
| 1 | Foundational (BOM, parent POM, core dependencies) |
| 2-3 | Dependency swaps, annotation type renames |
| 4 | Code-level annotation changes |
| 6 | Property/configuration changes |
| 7-8 | Method-level transformations |

## Provider Guidelines

- **OpenRewrite**: Deterministic, mechanical changes. Check reference file for recipes and GAVs.
- **AI**: Complex refactoring, no existing recipe. Single-step tasks with specific class/method names.
- **Manual**: Fallback summary. Use `"See openrewrite instructions"` for mechanical changes.

## Naming

- **File:** `<NNN>-descriptive-name.yaml` (zero-padded: 010, 020, 030...)
- **RuleID:** Same as filename without extension
- File numbers = sequence within the set. `order` field = execution order.