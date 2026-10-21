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
mvn package -pl mcp

// To run the IT tests
mvn verify -Pit -pl mcp
```

Next, launch the ACP client and pass the parameters to configure the Quarkus MCP server using stdio mode

## Filesystem access guardrail

The MCP server restricts tool access to a set of allowed directories, following the [MCP local server security](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/local-server-security#limit-filesystem-access) convention. The `sourceAnalyze` and `sourceValidate` tools will reject any path outside these boundaries.

**Default:** the current working directory (`.`) where the server is started. When you run `acp run` from the repository root, all relative paths like `./applications/...` are allowed automatically.

**Override:** pass `-Dmtool.allowed-dirs=<dir1>,<dir2>` in the MCP server args to restrict or expand access:

```shell
// Restrict to only the applications directory
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dmtool.allowed-dirs=./applications", "-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}'

// Allow access to multiple directories
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-Dmtool.allowed-dirs=./applications,/tmp/projects", "-jar", "./mcp/target/mcp-2.0.0-SNAPSHOT-runner.jar"]}'
```

## Transport: stdio

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

## Transport: HTTP

- Create a mcp JSON file or use the file `mcp-server.json` under the module: `mcp`
```json
{
  "type": "http",
  "name": "quarkus-tools",
  "url": "http://localhost:8080/mcp"
}
```
- Launch the Quarkus MCP server: `mvn quarkus:dev` and define the allowed directories: `-Dmtool.allowed-dirs=/path/to/migration-tool`
**NOTE**: To log the JSON-RPC messages, pass the following property: `-Dquarkus.mcp.server.traffic-logging.enabled=true`

Example:
```shell
cd mcp
mvn -Dmtool.allowed-dirs=/path/to/migration-tool -Dquarkus.mcp.server.traffic-logging.enabled=true quarkus:dev
```

- Execute one of the previous commands to test: `analyze` or `validate` tools but where you pass the path of the JSON file
```shell
acp run \
  -a claude-acp \
  --backup no \
  -s ./mcp/skills/<ANALYZE_OR_VALIDATE>/SKILLS.md \
  --mcp-server-config ./mcp/mcp-server.json \
  -p ...
```

- Example to analyze an application using the `analyze` SKILL
```shell
# Without MCP Tools
acp run -a claude-acp \
  -s ./mcp/skills/analyze/SKILL.md \
  --backup no \
  -p "Analyze the project: /path/to/migration-tool/applications/spring-jpa-crud"

# With MCP
acp run -a claude-acp \
  --mcp-server-config ./mcp/mcp-server.json \
  -s ./mcp/skills/analyze/SKILL.md \
  --backup no \
  -p "Analyze the project with tools: /path/to/migration-tool/applications/spring-jpa-crud"
```