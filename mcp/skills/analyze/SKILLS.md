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

## Critical rules

- Create at the beginning of the session a log file: **ai-analysis-log_yyyymmdd_hh:mm:ss.txt** using the template of the section reference: **ai-log.txt file**
- For every **action** you will take including **tool** that you as agent will use and execute, **log such information** within the log file
- Every line will include:
  - The action or thought description,
  - The tool executed when applicable
  - Why you decided to take such an action/decision

## MCP Tools (optional)

This section declares MCP tools that can be invoked during analysis. They are **only called when the user's prompt signals intent** — look for phrases such as:
- "analyze with tools", "use MCP", "use tools", "deep analysis", "full analysis"
- Any explicit mention of a tool name listed below

When none of these signals are detected, perform the standard analysis without calling any MCP tool.

### Rules

**- Don't list using agent tool the files under the project path**

### Declared tools

<!-- Add or remove MCP tools here. Each entry must specify: name, purpose, and arguments. -->

| Tool | Purpose | Arguments |
|------|---------|-----------|
| `sourceAnalyze` | Analyze source code of the project | `path`: path of the project |

### How to call

1. Before calling an MCP tool, log the intent in the analysis log (tool name, arguments, why).
2. Call the tool using `sourceTool()` with the tool name and arguments from the table above.
3. Log the result summary in the analysis log.
4. Incorporate the tool's output into your analysis.

### Log format for MCP tool calls

```
[ACTION N] Call MCP tool
  Tool: <TOOL_NAME>
  Arguments: <ARGS>
  Why: <REASON>
  Result: <SUMMARY OF OUTPUT>
```

### Tool Output format

- Report the **full raw results** from the tool — do NOT summarize or consolidate them.
- For each match, include the complete details: file path, line positions (start/end), text, target type (class/field/method/parameter), and the annotated element name.
- Group results by query type (classes, annotations, dependencies, etc.) using markdown headers.
- Present each match on its own line, preserving the original format returned by the tool.

## Log file Reference

**ai-analysis-log_yyyymmdd_hh:mm:ss.txt file**
```txt
AI Analysis Log - <TIMESTAMP>
Project: <PATH>
MCP Tools: <ENABLED | DISABLED>
================================================================================

[ACTION 1] <ACTION1>
  Tool: <TOOL>
  Why: <REASON1>

[ACTION 2] <ACTION2>
  Tool: <TOOL>
  Why: <REASON2>

...

[ACTION N] <ACTION N>
  Tool: <TOOL>
  Why: <REASON N>
  Result: <RESULT>    
```