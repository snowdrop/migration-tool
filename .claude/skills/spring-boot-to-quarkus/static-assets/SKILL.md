---
name: static-assets
description: Moves static assets from Spring Boot static/ directory to Quarkus META-INF/resources/ and updates JavaScript files.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 8: Static Assets & JavaScript

Move static assets to the Quarkus-expected location and update JavaScript files.

## Preconditions

Before executing this skill, verify the following conditions are met. **Skip this skill entirely if none of the preconditions match.**

- [ ] Static assets exist in `src/main/resources/static/`. 
- [ ] Or JavaScript/CSS files reference Spring-specific patterns (e.g., CSRF tokens).

If no static assets exist in the Spring Boot location, this skill does not apply.

## Migration Steps

- [ ] Move `src/main/resources/static/` → `src/main/resources/META-INF/resources/`
- [ ] Update `home.js`:
  - Remove CSRF token handling (Quarkus does not use CSRF tokens by default)
  - Verify AJAX endpoint URLs match the new JAX-RS paths
  - Update `Content-Type` headers if needed