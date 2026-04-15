---
name: service-layer
description: Removes unnecessary Spring service interfaces and implementations, moving logic to repository or resource classes.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 5: Service Layer Simplification

Remove unnecessary Spring service abstraction layers and move business logic to the repository or resource class.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains classes annotated with `@Service`. 

If no `@Service` classes are found, this skill does not apply.

## Instructions

- Locate all service classes and their interfaces.
- Understand the service methods and their callers.
- Move logic and remove files.

## Migration Steps

- [ ] Remove `TaskService.java` interface (unnecessary indirection)
- [ ] Remove `TaskServiceImpl.java`
- [ ] Move business logic directly into the repository or the resource class:

| Service method | Replacement |
|---|---|
| `addTask(task)` | `taskRepository.persist(task)` (called from resource) |
| `deleteTask(id)` | `taskRepository.deleteById(id)` (called from resource) |
| `getAllTasks()` | `taskRepository.listAll()` (called from resource) |
| `getAllTasksPage(pageNo, pageSize)` | `taskRepository.findAllPaged(...)` (on repository) |