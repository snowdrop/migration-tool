---
name: analyze-application
description: "Use when user says: analyze the code, analyze the application under the path"
---

## Instructions

- Analyze the application using the tool: 'sourceAnalyze' using as parameters:
  - project or application path 
  - the queries: 
    - 'java class all'
    - 'java annotation all'
    - 'java interface all'
    - 'pom dependency all'
    - 'properties all'

## Output format

- Report the **full raw results** from the tool — do NOT summarize or consolidate them.
- For each match, include the complete details: file path, line positions (start/end), text, target type (class/field/method/parameter), and the annotated element name.
- Group results by query type (classes, annotations, dependencies, etc.) using markdown headers.
- Present each match on its own line, preserving the original format returned by the tool.