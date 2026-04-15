---
name: repository-layer
description: Migrates Spring Data JpaRepository interfaces to Quarkus Panache PanacheRepository classes.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 4: Repository Layer

Migrate Spring Data JPA repositories to Quarkus Panache repositories.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] The project contains interfaces extending `JpaRepository` or `CrudRepository`.

If no Spring Data repositories are found, this skill does not apply.

## Instructions

- Locate all repository interfaces.
- Examine each repository file.
- Apply the transformations.

## Migration Steps

- [ ] Find the existing `Repository` class and implements `JpaRepository<Task, Long>`. Annotate the class with `@ApplicationScoped`
- [ ] Add imports:
  ```java
  import io.quarkus.hibernate.orm.panache.PanacheRepository;
  import jakarta.enterprise.context.ApplicationScoped;
  ```
- [ ] Add pagination method using Panache's `PanacheQuery` + `Page`:
  ```java
  public PanacheQuery<Task> findAllPaged(int pageNo, int pageSize) {
      return findAll().page(Page.of(pageNo - 1, pageSize));
  }
  ```
- [ ] Remove Spring Data imports (`org.springframework.data.*`)
- [ ] Create if it does not exist a new `@GET` endpoint able to reply to JSON request to get the list of the entities using the PanacheRepository.findAll() as JSON Response.
  Use the following code as example **where** you will have to **rename** what you will find within `<>` using the proper entity name: singular or plural and class implementing the PanacheRepository
  ```java
  @GET
    @Path("/<entities>")
    @Produces(MediaType.APPLICATION_JSON)
    public Response view<Entities>() {
        return Response.ok(PanacheRepository.findAll().list()).build();
    }
  ```
## Conversion Example

### Before (Spring Data JPA)

```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
```

### After (Panache)

```java
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    public PanacheQuery<Task> findAllPaged(int pageNo, int pageSize) {
        return findAll().page(Page.of(pageNo - 1, pageSize));
    }
}
```