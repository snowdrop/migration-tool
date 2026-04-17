---
name: templates
description: Migrates Thymeleaf templates to Quarkus Qute syntax (th:each to {#for}, th:text to expressions, etc.).
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 7: Templates (Thymeleaf → Qute)

Migrate Thymeleaf templates to Quarkus Qute template syntax.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] Template files exist in `src/main/resources/templates/`. 
- [ ] Templates contain Thymeleaf-specific syntax.

If no Thymeleaf templates are found, this skill does not apply.

## Instructions

- Locate all template files.
- Examine each template.
- Convert Thymeleaf syntax to Qute syntax.

## Migration Steps

- [ ] Check that we have this quarkus dependency: `quarkus-rest-qute`
- [ ] Rewrite `src/main/resources/templates/home.html` as a Qute template
- [ ] Convert Thymeleaf syntax to Qute syntax using the conversion table below
- [ ] Create `TemplateDateExtensions.java` with `@TemplateExtension` for date formatting
- [ ] Convert `error.html` to Qute syntax
- [ ] Move static files to `src/main/resources/META-INF/resources/js/home.js`

## Important: Qute Strict Data Map

Unlike Thymeleaf, which silently treats missing variables as `null`/`false`, **Qute requires every key referenced in the template to exist in the data map** — even keys used only inside `{#if}` guards. If a key like `totalPages` appears in `{#if totalPages > 1}` but is not passed via `.data("totalPages", ...)`, Qute throws a `TemplateException`.

**Action:** After converting templates, collect **every** key referenced in the template (in `{#if}`, `{#for}`, `{expressions}`, etc.) and ensure the corresponding resource class passes the **complete set of keys on every code path**. For example, if the template uses `tasks`, `noTasks`, `totalPages`, `currentPage`, and `totalItems`, then both the empty-result path (`.data("tasks", List.of()).data("totalPages", 0).data("currentPage", 1).data("totalItems", 0L).data("noTasks", true)`) and the has-results path (`.data("noTasks", false)`) must include all keys.

## Syntax Conversion Table

| Thymeleaf | Qute |
|---|---|
| `th:each="task : ${tasks}"` | `{#for task in tasks}...{/for}` |
| `th:text="${task.title}"` | `{task.title}` |
| `th:if="${condition}"` | `{#if condition}...{/if}` |
| `th:unless="${condition}"` | `{#if !condition}...{/if}` |
| `th:href="@{'/home/' + ${pageNo}}"` | `href="/home/{pageNo}"` |
| `th:attr="data-task-id=${task.id}"` | `data-task-id="{task.id}"` |
| `th:field="${task.title}"` | `name="title" value="{task.title}"` |
| `th:action="@{/home}"` | `action="/home"` |
| `th:object="${task}"` | Remove (not needed) |
| `th:src="@{/js/home.js}"` | `src="/js/home.js"` |
| `${#temporals.format(task.dueDate, '...')}` | `{task.dueDate.format('dd MMMM yyyy')}` via `@TemplateExtension` |
| `${#numbers.sequence(1, totalPages)}` | `{#for i in totalPages}` with `{i_count}` |
| `[[${i}]]` | `{i_count}` |