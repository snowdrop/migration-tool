---
name: analyze-application
description: "Use when user says: analyze the code, analyze the application under the path"
---

## Instructions

- Analyze the application using the tool: 'quarkit_analyze' using as parameters:
  - project or application path 
  - the queries: 
    - 'java class all'
    - 'java annotation all'
    - 'java interface all'
    - 'pom dependency all'
    - 'properties all'
- Then, for each annotation, dependency and property found by `quarkit_analyze`, call the tool `quarkit_reference_lookup` (do NOT read the reference files directly) using as parameters:
  - `map`: `annotation` for annotations, `dependency` for pom dependencies, `config` for properties
  - `key`: the Spring annotation (e.g. `@Primary`), dependency artifactId (e.g. `spring-boot-starter-web`) or property name (e.g. `server.port`)
  - `mode`: `compat` for a short answer or `full` (default) to also get the section and notes
- Use `quarkit_references_list` to discover the available maps and their sections.
- The `compat` field of each row tells how the Spring construct is handled with the Quarkus Spring compatibility extensions: `SUPPORTED`, `SUPPORTED_WITH_CHANGE` or `NOT_SUPPORTED` (full migration to the `quarkus` value is required).

## Output format

- Report the **full raw results** from the tool — do NOT summarize or consolidate them.
- For each match, include the complete details: file path, line positions (start/end), text, target type (class/field/method/parameter), and the annotated element name.
- Group results by query type (classes, annotations, dependencies, etc.) using markdown headers.
- Present each match on its own line, preserving the original format returned by the tool.