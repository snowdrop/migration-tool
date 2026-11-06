# Quarkus MCP Server and tools

Quarkus MCP server supporting stdio and HTTP modes and Tools able to analyze the source files, validate, etc.

## How to use the MCP server & Tools

Before to execute the commands described hereafter, you must install the Smallrye ACP Client (>= 0.2.1):
```shell
jbang app install --name acp io.smallrye.ai:acp-java-client:0.2.1:runner

// To get the list of the ACP Agents ...
acp registry list -r 
ACP Registry v1.0.0 - 41 agents available
Current platform: darwin-aarch64

ID                        VERSION      DISTRIBUTION   DESCRIPTION
------------------------------------------------------------------------------------------
agoragentic-acp           1.3.0        npx            Agent marketplace with 174+ AI capa...
amp-acp                   0.9.0        binary         ACP wrapper for Amp - the frontier ...
antigravity-acp           1.3.0        binary         Google’s AI coding agent
auggie                    0.36.0       npx            Augment Code's powerful software ag...
autohand                  0.2.1        npx            Autohand Code - AI coding agent pow...
..

// Install an agent
acp registry install claude-acp
```

Compile the Quarkus MCP server first
```shell
mvn install -pl references,mcp

// To run the IT tests
mvn clean verify -Pit -pl mcp
```

Next, launch the ACP client and pass the parameters to configure the Quarkus MCP server using stdio mode

## Filesystem access guardrail

The MCP server restricts tool access to a set of allowed directories, following the [MCP local server security](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/local-server-security#limit-filesystem-access) convention. The `quarkit_analyze` and `quarkit_validate` tools will reject any path outside these boundaries.

**Default:** the current working directory (`.`) where the server is started. When you run `acp run` from the repository root, all relative paths like `./applications/...` are allowed automatically.

**Override:** pass `-Dmtool.allowed-dirs=<dir1>,<dir2>` in the MCP server args to restrict or expand access:

```shell
// Restrict to only the applications directory
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dmtool.allowed-dirs=./applications", "-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}'

// Allow access to multiple directories
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dmtool.allowed-dirs=./applications,/tmp/projects", "-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}'
```

## Spring to Quarkus references

The server loads the Spring to Quarkus annotation, config and dependency maps (JSON files generated from the [quarkusio/skills](https://github.com/quarkusio/skills/tree/main/skills/migrate-spring-to-quarkus/references) Markdown tables with `python3 scripts/convert_references.py`) at startup and exposes them through:

| Tool | Description |
|---|---|
| `quarkit_references_list` | Lists the available maps (`annotation`, `config`, `dependency`) and their sections |
| `quarkit_reference_lookup(map, key, mode)` | Returns the matching rows as JSON. `key` is an exact (case-insensitive) or substring match; `mode` is `full` (default) or `compat` (spring, quarkus, compat only). Each row has `section`, `spring`, `quarkus`, `compat` (`SUPPORTED`, `NOT_SUPPORTED`, `SUPPORTED_WITH_CHANGE`) and `notes` |

The same data is available as MCP resources: `mtool://references/annotation-map`, `mtool://references/config-map`, `mtool://references/dependency-map` and the resource template `mtool://references/{map}/{key}`.

### `references` module

The `references` module packages the three JSON map files into a standalone jar (`dev.snowdrop.mtool:references`), under `references/` in the jar root. It contains no Java classes — only resources.

| File | Records |
|---|---|
| `references/annotation-map.json` | 99 |
| `references/config-map.json` | 54 |
| `references/dependency-map.json` | 37 |

The maps are **not embedded** in the MCP server jar (the `references` dependency of the `mcp` module has the `test` scope): they must be provided at runtime, either as a Maven dependency on the classpath (`jbang --deps`) or with the `quarkit.references.path` property. Otherwise the server starts with a `Reference file not found` warning and the reference tools return empty results.

Build and install the jar in the local Maven repository:
```shell
mvn install -pl references
// Output: references/target/references-2.0.0-SNAPSHOT.jar
```

### Provide the references as a Maven dependency with `jbang --deps`

jbang resolves the `references` artifact from the Maven repositories (local `~/.m2` included) and adds it to the classpath of the MCP server:

```shell
// Using the jbang catalog of the project (alias: mcp, catalog: mtool)
jbang catalog add --name quarkit https://github.com/snowdrop/migration-tool/blob/main/jbang-catalog.json
jbang --deps dev.snowdrop.mtool:references:2.0.0-SNAPSHOT mcp@quarkit

// Or using the runner jar directly
jbang --deps dev.snowdrop.mtool:references:2.0.0-SNAPSHOT ./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar
```

Use `--repos` to resolve the artifact from another Maven repository, e.g. `--repos central,snapshots=https://central.sonatype.com/repository/maven-snapshots/`.

#### Register the catalog locally, using the local Maven repository

Until the jars are published as early access, use the `jbang-catalog-dev.json` catalog. Its aliases point to Maven coordinates (`dev.snowdrop.mtool:mcp:2.0.0-SNAPSHOT:runner`, `dev.snowdrop.mtool:migration-cli:2.0.0-SNAPSHOT:runner`) which jbang resolves from the local Maven repository (`~/.m2`), and the `mcp` alias declares the `references` jar as dependency, so `--deps` is not needed.

```shell
// 1. Install the jars in ~/.m2 (the migration-cli must be packaged as an uber-jar to get the runner jar)
mvn install -DskipTests -pl references,mcp
mvn install -DskipTests -pl migration-cli -Dquarkus.package.jar.type=uber-jar

// 2. Register the catalog globally. Use a file:// URL: jbang only accepts a local path when the file is named jbang-catalog.json
jbang --fresh catalog add --global --name quarkit file://$(pwd)/jbang-catalog-dev.json

// 3. Launch the MCP server (or the client: jbang mtool@quarkit)
jbang mcp@quarkit
```

jbang caches the catalogs read from a URL: after changing `jbang-catalog-dev.json`, run `jbang --fresh catalog update` (or remove and add the catalog again). A new `mvn install` of the jars is picked up without updating the catalog. To unregister it: `jbang catalog remove --global quarkit`.

Without registering it, the catalog can also be used with `--catalog` (run it from a folder having no `mcp` subfolder, otherwise jbang tries to run this folder):
```shell
jbang --catalog <repo>/jbang-catalog-dev.json mcp
```

In the MCP client config:
```shell
--mcp-server-config '{"type":"stdio","name":"mtools","command":"jbang","args":["--deps","dev.snowdrop.mtool:references:2.0.0-SNAPSHOT","mcp@mtool"]}'
```

### Override the references with `quarkit.references.path`

Point `quarkit.references.path` to a directory, or to a jar/zip, containing `annotation-map.json`, `config-map.json` and `dependency-map.json` (in a jar, at the root or under `references/`). A file found there takes precedence over the one available on the classpath; a missing file falls back to the classpath.

```shell
// Point to the references jar
jbang -Dquarkit.references.path=./references/target/references-2.0.0-SNAPSHOT.jar mcp@mtool

// Or as an environment variable in the MCP client config
--mcp-server-config '{"type":"stdio","name":"mtools","command":"jbang","args":["mcp@mtool"],"env":{"MTOOL_REFERENCES_PATH":"./references/target/references-2.0.0-SNAPSHOT.jar"}}'

// Or point to a plain directory containing the JSON files
jbang -Dquarkit.references.path=/path/to/references-dir mcp@mtool
```

## stdio

### Analyze Tool

```shell
// At the root of the GitHub repository
acp run \
  -a claude-acp \
  --backup no \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}' \
  -p "The Project path of the code to analyze using the MCP tool: sourceAnalyze is: ./applications/spring-boot-todo-app"
```

### Validate tool

```shell
acp run \
  -a claude-acp \
  --backup no \
  -s ./mcp/skills/validate/SKILLS.md \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}' \
  -p "Validate the migration of the spring project: ./applications/spring-jpa-crud against the quarkus project: ./applications/spring-jpa-crud-quarkus. Only run the validator: persistence"
```

## To be reviewed

**claude**

```shell
# Using a SKILL
acp run \
  -a claude-acp \
  --backup no \
  -s ./mcp/skills/analyze/SKILLS.md \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}' \
  -p "Analyze the application: ./applications/quarkus-todo-demo-app"
```

**IBM bob**

```shell
# Using a SKILL
acp run \
  -a bob \
  --backup no \
  -s ./mcp/skills/analyze/SKILLS.md \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}' \
  -p "Analyze the application: ./applications/quarkus-todo-demo-app"
```