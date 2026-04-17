---
name: entity-layer
description: Migrates JPA entities from Spring Data style to Quarkus Panache (extend PanacheEntity, public fields).
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 3: Entity Layer

Migrate JPA entity classes from Spring Data JPA style to Quarkus Panache conventions.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains classes annotated with `@Entity`. 
- [ ] Entity classes do **not** already extend `PanacheEntity`. 
  If matches are found, entities are already migrated — skip this skill.

## Instructions

- Locate all `@Entity` classes.
- Examine each entity file.
- Apply the transformations.

## Migration Steps

- [ ] Change entity class to extend `PanacheEntity`
- [ ] Add import: `import io.quarkus.hibernate.orm.panache.PanacheEntity;`
- [ ] Remove manual `@Id` and `@GeneratedValue` fields (provided by `PanacheEntity`)
- [ ] Convert private fields + getters/setters to public fields (Panache convention)
- [ ] Keep `@Entity` and `@Table` annotations
- [ ] Remove `@DateTimeFormat` (Spring-specific; use Qute formatting in templates)
- [ ] Verify entity compiles

## Conversion Example

### Before (Spring Data JPA)

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate dueDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
```

### After (Panache)

```java
@Entity
@Table(name = "tasks")
public class Task extends PanacheEntity {
    public String title;
    public LocalDate dueDate;
}
```