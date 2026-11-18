# Quarkus MCP Server and tools

Quarkus MCP server exposing the following MCP Tools using stdio or HTTP mode.

| Tool | Description | Section |
|---|---|---|
| `quarkit_analyze` | Analyzes a project to discover its Java classes, annotations, POM dependencies and properties | [Analyze Tool](#analyze-tool) |
| `quarkit_validate` | Validates that a Spring to Quarkus migration is correct and complete | [Validate tool](#validate-tool) |
| `quarkit_references_list` | Lists the available Spring to Quarkus reference maps and their sections | [References tool](#references-tool) |
| `quarkit_reference_lookup` | Looks up how a Spring annotation, configuration property or dependency maps to Quarkus | [References tool](#references-tool) |

## Compile the MCP server

The MCP transport extensions packaged within the uber-jar are selected using the property `mcp.transport`, which activates one of the following Maven profiles:

| Profile | Command | Transports | Uber-jar | Usage |
|---|---|---|---|---|
| `all` (default) | `mvn clean install` | `stdio` + `http` | `mcp/target/mcp-<version>-runner.jar` | Local development: `mvn quarkus:dev -pl mcp` |
| `stdio` | `mvn clean install -Dmcp.transport=stdio` | `stdio` | `mcp/target/mcp-<version>-stdio.jar` | Released |
| `http` | `mvn clean install -Dmcp.transport=http` | `http` | `mcp/target/mcp-<version>-http.jar` | Released |

**Note**: The default `-runner.jar` also starts an HTTP server on port `8080` when launched in `stdio` mode by an MCP client. Build the `stdio` uber-jar (`-Dmcp.transport=stdio`) to only use the stdio transport.

The two uber-jars are released on Maven Central under the GAV `dev.snowdrop.mtool:mcp:<version>` with the classifiers `stdio` and `http`.

To run the IT tests of the Quarkus MCP server (compiled as described above):
```shell
mvn clean verify -Pit -pl mcp
```

## Start the MCP server

Using `java -jar`:
```shell
# stdio transport (compiled with -Dmcp.transport=stdio)
java -jar ./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar

# HTTP transport (compiled with -Dmcp.transport=http)
java -jar ./mcp/target/mcp-2.0.2-SNAPSHOT-http.jar
```

Or using `jbang` and the [`jbang-catalog.json`](../jbang-catalog.json) of the project (aliases: `mcp` for `stdio` and `mcp-http` for HTTP):
```shell
# stdio transport
jbang app install mcp@quarkit
jbang mcp@quarkit

# HTTP transport
jbang app install mcp-http@quarkit
jbang mcp-http@quarkit
```

Using the HTTP transport, the endpoint is then available at `http://localhost:8080/mcp`.

## Query the MCP server

### Option 1: curl

**Note**: Requires the MCP server started with the HTTP transport (see [Start the MCP server](#start-the-mcp-server)).

The server is stateless: each request must include the `MCP-Protocol-Version`, `Mcp-Method` (and `Mcp-Name` for `tools/call`) headers and the `_meta` object.

List all the tools:
```shell
curl -s http://localhost:8080/mcp \
        -H 'Content-Type: application/json' \
        -H 'Accept: application/json, text/event-stream' \
        -H 'MCP-Protocol-Version: 2026-07-28' \
        -H 'Mcp-Method: tools/list' \
        -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{"_meta":{"io.modelcontextprotocol/protocolVersion":"2026-07-28","io.modelcontextprotocol/clientInfo":{"name":"curl","version":"1.0"},"io.modelcontextprotocol/clientCapabilities":{}}}}' | jq -r .
```

Analyze a project:
```shell
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: tools/call' \
  -H 'Mcp-Name: quarkit_analyze' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"quarkit_analyze","arguments":{"query":"java class all;java annotation all","project_path":"./applications/spring-boot-todo-app"},"_meta":{"io.modelcontextprotocol/protocolVersion":"2026-07-28","io.modelcontextprotocol/clientInfo":{"name":"curl","version":"1.0"},"io.modelcontextprotocol/clientCapabilities":{}}}}'
```

### Option 2: ACP client

The [ACP client](https://github.com/smallrye/smallrye-acp-client) is a Java client for the [Agent Client Protocol (ACP)](https://agentclientprotocol.com/get-started/introduction). It lets you talk to different LLMs through an `ACP` agent. With it, you can:

- send prompt messages to the agent (`-p`),
- provide skills to guide the agent (`-s`),
- declare how the agent reaches the Quarkus MCP server, either over HTTP or by launching it in `stdio` mode (`--mcp-server-config`).

Before using the `acp` client, install it and then install an `ACP` agent using the following commands:
```shell
# Latest version defined in the catalog
jbang app install acp@smallrye/smallrye-acp-client

# To get the list of the ACP Agents ...
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

# To install an agent
acp registry install claude-acp
```

You can then launch the ACP client and configure its parameters, such as `--mcp-server-config`, so that the agent can use the Quarkus MCP server during the conversation.

**Note**: You don't have to start the Quarkus MCP server separately. Instead, you can tell the ACP client how to launch it (using `java -jar` or JBang) with the `stdio` uber-jar, as shown in the examples of the [Tools](#tools) section.

## Tools

This section details the different `MCP Tool`, their parameters and how to interact with them using `acp run` command.

**Note**: The list of the commands is not exhaustive and can be expanded ;-)

### Analyze Tool

Tool name: `quarkit_analyze`

Analyze the source to discover the java classes, annotations, properties, etc files. Accepts multiple queries separated by semicolons, e.g. `java class all;java annotation all`.

| Parameter | Required | Default | Description |
|---|---|---|---|
| `query` | yes | | Queries to perform, separated by `;`. Each query has 3 parts: `<fileType> <symbol> <operation>`. Example: `java class all;java annotation all;pom dependency all;properties all` |
| `project_path` | no | `.` | Project path |

The tool access is restricted to the allowed directories (see [Filesystem access guardrail](#filesystem-access-guardrail)).

```shell
# At the root of the GitHub repository
acp run \
  -a claude-acp \
  --backup no \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar"]}' \
  -p "The Project path of the code to analyze using the MCP tool: sourceAnalyze is: ./applications/spring-boot-todo-app"
```

### Validate tool

Tool name: `quarkit_validate`

Validate that a Spring to Quarkus migration is correct and complete. Runs validators (project-setup, persistence, messaging, REST, transaction, etc) against the migrated Quarkus project, comparing it with the original Spring project to detect missing entities, field mismatches, ID strategy changes, and relationship differences.

| Parameter | Required | Default | Description |
|---|---|---|---|
| `target` | yes | | Path to the migrated Quarkus project to validate |
| `source` | yes | | Path to the original Spring project (required for persistence validation) |
| `validator` | no | `all` | Validator to run: `project-setup`, `persistence`, or `all` |
| `skipCompile` | no | `false` | Skip the `mvn compile` check |
| `parser` | no | `treesitter` | Java parser to use: `treesitter` (default) or `javaparser` |

The tool access is restricted to the allowed directories (see [Filesystem access guardrail](#filesystem-access-guardrail)) and the `validator` value is checked before execution.

```shell
acp run \
  -a claude-acp \
  --backup no \
  -s ./mcp/skills/validate/SKILLS.md \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar"]}' \
  -p "Validate the migration of the spring project: ./applications/spring-jpa-crud against the quarkus project: ./applications/spring-jpa-crud-quarkus. Only run the validator: persistence"
```

### References tool

#### `quarkit_references_list`

List the available Spring to Quarkus reference maps (`annotation`, `config`, `dependency`) with their sections and number of records.

This tool has no parameter but user can override using the property `quarkit.references.path` the path to point to a directory, or to a jar/zip, containing the JSON files. In a jar, at the root or under `references/`. A file found there takes precedence over the one available on the classpath; a missing file falls back to the classpath.

```shell
// Point to the references jar
jbang -Dquarkit.references.path=./references/target/references-2.0.2-SNAPSHOT.jar mcp@quarkit

// Define the jar as jbang dependency
jbang --deps dev.snowdrop.mtool:references:2.0.2-SNAPSHOT mcp@quarkit

// Or point to a plain directory containing the JSON files
jbang -Dquarkit.references.path=/path/to/references-dir mcp@quarkit
```

#### `quarkit_reference_lookup`

Look up how a Spring annotation, configuration property or dependency is migrated to Quarkus. Returns the matching rows as JSON: `section`, `spring`, `quarkus`, `compat` (`SUPPORTED`, `NOT_SUPPORTED` or `SUPPORTED_WITH_CHANGE` with the Quarkus Spring compatibility extensions) and `notes`.

At least one of `section` or `key` must be provided:
- `map` + `section`: returns the list of all the rows of the section
- `map` + `key`: returns the rows matching the key in the whole map
- `map` + `section` + `key`: returns the rows matching the key within the section

| Parameter | Required | Default | Description |
|---|---|---|---|
| `map` | yes | | Reference map to search: `annotation`, `config` or `dependency` |
| `section` | no | | Section of the map (see `quarkit_references_list`), e.g. `Dependency Injection`. Exact match (case-insensitive) is preferred, otherwise a substring match is used |
| `key` | no | | Spring annotation, property or dependency to look for, e.g. `@Primary`. Exact match (case-insensitive) is preferred, otherwise a substring match is used. Restricted to `section` when provided |
| `mode` | no | `full` | `full` returns all fields, `compat` returns only `spring`, `quarkus` and `compat` |

An error JSON (`{"error": "..."}`) is returned when the `map` is unknown, when neither `section` nor `key` is provided, or when the `mode` is not `full` or `compat`.

Example of tool arguments:

```json
{ "map": "annotation", "section": "Dependency Injection", "key": "@Primary", "mode": "compat" }
```

See [`cmds.md`](cmds.md) for ready-to-use HTTPie queries.

#### Resources

The reference JSON data are also available as MCP resources: 
- `mtool://references/annotation-map`, 
- `mtool://references/config-map`, 
- `mtool://references/dependency-map` 
and the resource template `mtool://references/{map}/{key}`, where `{key}` is either a Spring key or the name of a section (which returns all its rows).

## Filesystem access guardrail

The MCP server restricts `tool access` to a set of allowed directories, following the [MCP local server security](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/local-server-security#limit-filesystem-access) convention. The `quarkit_analyze` and `quarkit_validate` tools will reject any path outside these boundaries.

By default, when we start the MCP server, the current working directory (`.`) will be set as `auhtorized` path to access files when running the tools.
To override, pass the following system property `-Dquarkit.allowed-dirs=<dir1>,<dir2>` or the environment variable `QUARKIT_ALLOWED_DIRS=<dir1>,<dir2>` to expand access (the same applies to `quarkit.references.path` / `QUARKIT_REFERENCES_PATH`)

```shell
// Restrict to only the applications directory
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dquarkit.allowed-dirs=./applications", "-jar", "./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar"]}'

// Allow access to multiple directories
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dquarkit.allowed-dirs=./applications,/tmp/projects", "-jar", "./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar"]}'

// HTTP server registered in the acp registry and launched with: acp mcp serve
acp mcp serve -e QUARKIT_ALLOWED_DIRS=./applications,/tmp/projects -e QUARKIT_REFERENCES_PATH=./references/target/references-2.0.2-SNAPSHOT.jar quarkit-http
```