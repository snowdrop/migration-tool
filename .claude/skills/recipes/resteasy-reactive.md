# RESTEasy Classic to RESTEasy Reactive — Recipe Reference

Rules directory: `cookbook/rules/quarkus-resteasy-reactive/`

## Existing rules

| File | RuleID | Order | What it does |
|------|--------|-------|-------------|
| 010-replace-resteasy-deps.yaml | 010-replace-resteasy-deps | 1 | Replace quarkus-resteasy* deps with quarkus-rest* equivalents |
| 020-update-jaxrs-path-param-annotation.yaml | 020-update-jaxrs-response | 2 | Replace 10 RESTEasy Classic annotations with Reactive equivalents |
| 030-update-properties.yaml | 030-update-properties | 3 | Rename quarkus.resteasy.* properties to quarkus.rest.* |

## Common conditions for this scenario

```yaml
# Dependency-based triggers
pom.dependency is (gavs='io.quarkus:quarkus-resteasy') OR
pom.dependency is (gavs='io.quarkus:quarkus-resteasy-jackson') OR
pom.dependency is (gavs='io.quarkus:quarkus-resteasy-jsonb') OR
pom.dependency is (gavs='io.quarkus:quarkus-resteasy-jaxb') OR
pom.dependency is (gavs='io.quarkus:quarkus-resteasy-qute')

# Annotation-based triggers
java.annotation is 'org.jboss.resteasy.annotations.jaxrs.PathParam' OR
java.annotation is 'org.jboss.resteasy.annotations.jaxrs.QueryParam'

# Property-based triggers
properties.key is 'quarkus.resteasy.*'
```

## Dependency mapping

| RESTEasy Classic | RESTEasy Reactive |
|---|---|
| quarkus-resteasy | quarkus-rest |
| quarkus-resteasy-jackson | quarkus-rest-jackson |
| quarkus-resteasy-jsonb | quarkus-rest-jsonb |
| quarkus-resteasy-jaxb | quarkus-rest-jaxb |
| quarkus-resteasy-qute | quarkus-rest-qute |

## Annotation mapping

| RESTEasy Classic | RESTEasy Reactive |
|---|---|
| org.jboss.resteasy.annotations.jaxrs.PathParam | org.jboss.resteasy.reactive.RestPath |
| org.jboss.resteasy.annotations.jaxrs.QueryParam | org.jboss.resteasy.reactive.RestQuery |
| org.jboss.resteasy.annotations.jaxrs.FormParam | org.jboss.resteasy.reactive.RestForm |
| org.jboss.resteasy.annotations.jaxrs.HeaderParam | org.jboss.resteasy.reactive.RestHeader |
| org.jboss.resteasy.annotations.jaxrs.CookieParam | org.jboss.resteasy.reactive.RestCookie |
| org.jboss.resteasy.annotations.jaxrs.MatrixParam | org.jboss.resteasy.reactive.RestMatrix |
| org.jboss.resteasy.annotations.cache.Cache | org.jboss.resteasy.reactive.Cache |
| org.jboss.resteasy.annotations.cache.NoCache | org.jboss.resteasy.reactive.NoCache |
| org.jboss.resteasy.annotations.SseElementType | org.jboss.resteasy.reactive.RestStreamElementType |
| org.jboss.resteasy.annotations.Separator | org.jboss.resteasy.reactive.Separator |

## Property mapping

| Classic | Reactive |
|---|---|
| quarkus.resteasy.path | quarkus.rest.path |
| quarkus.resteasy.gzip.enabled | quarkus.rest.gzip.enabled |
| quarkus.resteasy.gzip.max-input | quarkus.rest.gzip.max-input |

## GAV coordinates

- `org.openrewrite:rewrite-java:8.73.0`
- `org.openrewrite:rewrite-maven:8.73.0`

## Next available file number: 040