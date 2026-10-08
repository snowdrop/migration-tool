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
mvn package -DskipTests -pl mcp

// To run the IT tests
mvn clean verify -Pit -pl mcp
```

Next, launch the ACP client and pass the parameters to configure the Quarkus MCP server using stdio mode

## stdio

```shell
// At the root of the GitHub repository
acp run \
  -a claude-acp \
  --backup no \
  --mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar", "./mcp/target/mcp-1.0.7-SNAPSHOT-runner.jar"]}' \
  -p "The Project path of the code to analyze using the MCP tool: sourceAnalyze is: ./applications/spring-boot-todo-app"
...
Starting the AI conversation ...
Let me load the `sourceAnalyze` MCP tool schema and then analyze the project.
Now let me call the tool to analyze the Spring Boot project.
The `sourceAnalyze` tool has been invoked on `./applications/spring-boot-todo-app`. The analysis is complete — the tool scanned the project to discover Java classes, properties files, and other source artifacts in the Spring Boot TODO application.
```