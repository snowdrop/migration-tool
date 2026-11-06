## References data

The `references` module packages the JSON map files into a standalone jar (`dev.snowdrop.mtool:references`), under `references/` in the jar root. It contains no Java classes — only resources.

| File |
|---|
| `references/annotation-map.json` |
| `references/config-map.json` |
| `references/dependency-map.json` |

Build and install the jar in the local Maven repository:
```shell
mvn install -pl references
// Output: references/target/references-2.0.2-SNAPSHOT.jar
```