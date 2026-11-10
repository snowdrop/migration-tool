# Query the MCP server with HTTPie

Start the Quarkus MCP server (HTTP endpoint: `http://localhost:8080/mcp`):
```shell
jbang mcp-http@quarkit
```

Define the common headers and `_meta` once (fish shell):
```shell
set ACCEPT 'Accept:application/json, text/event-stream'
set VERSION 'MCP-Protocol-Version:2026-07-28'
set META '"_meta":{"io.modelcontextprotocol/protocolVersion":"2026-07-28","io.modelcontextprotocol/clientInfo":{"name":"httpie","version":"1.0"},"io.modelcontextprotocol/clientCapabilities":{}}'
```

## Tools

List the tools:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/list \
  jsonrpc=2.0 id:=1 method=tools/list params:="{$META}"
```

## References

List the reference maps (annotation, config, dependency) with their sections and number of records:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_references_list \
  jsonrpc=2.0 id:=2 method=tools/call \
  params:="{\"name\":\"quarkit_references_list\",\"arguments\":{},$META}"
```

List the reference resources and the resource template:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/list \
  jsonrpc=2.0 id:=3 method=resources/list params:="{$META}"

http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/templates/list \
  jsonrpc=2.0 id:=4 method=resources/templates/list params:="{$META}"
```

Read a whole map (`annotation-map`, `config-map` or `dependency-map`):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/read Mcp-Name:mtool://references/annotation-map \
  jsonrpc=2.0 id:=5 method=resources/read \
  params:="{\"uri\":\"mtool://references/annotation-map\",$META}"
```

## Query the annotation reference

Look up `@Primary` (full mode: section, spring, quarkus, compat, notes):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=6 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"annotation\",\"key\":\"@Primary\"},$META}"
```

Compat mode (spring, quarkus, compat only):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=7 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"annotation\",\"key\":\"@Primary\",\"mode\":\"compat\"},$META}"
```

List all the rows of a section (`map` + `section`):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=9 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"annotation\",\"section\":\"Dependency Injection\"},$META}"
```

Look up a key within a section (`map` + `section` + `key`):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=10 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"annotation\",\"section\":\"Dependency Injection\",\"key\":\"@Primary\"},$META}"
```

Using the resource template `mtool://references/{map}/{key}`:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/read Mcp-Name:mtool://references/annotation/@Primary \
  jsonrpc=2.0 id:=8 method=resources/read \
  params:="{\"uri\":\"mtool://references/annotation/@Primary\",$META}"
```

Using the resource template with a section name as `{key}` (returns all the rows of the section):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/read "Mcp-Name:mtool://references/annotation/Dependency Injection" \
  jsonrpc=2.0 id:=11 method=resources/read \
  params:="{\"uri\":\"mtool://references/annotation/Dependency Injection\",$META}"
```

## Query the dependency reference

Look up a Spring dependency in compat mode:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=12 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"dependency\",\"key\":\"spring-boot-starter-web\",\"mode\":\"compat\"},$META}"
```

Read the whole dependency map:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/read Mcp-Name:mtool://references/dependency-map \
  jsonrpc=2.0 id:=13 method=resources/read \
  params:="{\"uri\":\"mtool://references/dependency-map\",$META}"
```

## Query the config reference

Look up the Spring properties containing `spring.datasource` (substring match):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=14 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"config\",\"key\":\"spring.datasource\"},$META}"
```

Read the whole config map:
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:resources/read Mcp-Name:mtool://references/config-map \
  jsonrpc=2.0 id:=15 method=resources/read \
  params:="{\"uri\":\"mtool://references/config-map\",$META}"
```

## Error cases

Unknown map (returns `{"error": "Unknown map ..."}`):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=16 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"unknown\",\"key\":\"x\"},$META}"
```

Neither `section` nor `key` provided (returns `{"error": "At least one of the 'section' or 'key' arguments must be provided"}`):
```shell
http POST :8080/mcp "$ACCEPT" "$VERSION" Mcp-Method:tools/call Mcp-Name:quarkit_reference_lookup \
  jsonrpc=2.0 id:=17 method=tools/call \
  params:="{\"name\":\"quarkit_reference_lookup\",\"arguments\":{\"map\":\"annotation\"},$META}"
```

Notes:
- `:=` sends raw JSON (number, object), `=` sends a string.
- In a script or a pipe, add `--ignore-stdin`, otherwise HTTPie refuses to mix stdin with the `key=value` items.
- Missing `Mcp-Method`/`Mcp-Name` headers return `-32020 Missing required header`, a missing `_meta` returns `-32602 Stateless request must include _meta ...`, and without the `MCP-Protocol-Version` header the server expects the `initialize` handshake of the previous protocol versions.

# Transport modes: stdio and HTTP

The server is released as 2 distinct uber-jars, each embedding only one MCP transport extension. The transport is selected at build time using the property `mcp.transport` (Maven profiles `stdio` and `http` of the `mcp/pom.xml`), so no runtime property is needed to toggle the transports. Without the property, the profile `all` packages both transports (local development only, `mcp/target/mcp-<version>-runner.jar`, never released):

| Mode | Build | Uber-jar (local) | GAV (Maven Central) | JBang alias |
|---|---|---|---|---|
| stdio | `mvn package -pl mcp -Dmcp.transport=stdio` | `mcp/target/mcp-<version>-stdio.jar` | `dev.snowdrop.mtool:mcp:<version>:stdio` | `mcp@quarkit` |
| HTTP | `mvn package -pl mcp -Dmcp.transport=http` | `mcp/target/mcp-<version>-http.jar` | `dev.snowdrop.mtool:mcp:<version>:http` | `mcp-http@quarkit` |

Notes:
- The stdio uber-jar doesn't contain any HTTP server: no port is bound, so there is no conflict on `8080` (no `Listening on:` log line).
- The HTTP uber-jar keeps running when stdin is closed (background process, container, script).
- With the stdio uber-jar, the server stops as soon as stdin reaches EOF (e.g. `</dev/null` or a background process): `mcp stopped in 0.021s`.
- Logs are written to stderr (`quarkus.log.console.stderr=true`), so stdout only contains the JSON-RPC messages.
- Both jars are generated in `mcp/target`: build them one after the other **without** `clean` to keep both locally (as done below).

The commands below use the fish shell syntax and the local jars (run them from the project root). Replace `java -jar $STDIO_JAR` with `jbang mcp@quarkit` and `java -jar $HTTP_JAR` with `jbang mcp-http@quarkit` to test the released versions.
```shell
mvn -q package -pl mcp -DskipTests -Dmcp.transport=http   # mcp-2.0.2-SNAPSHOT-http.jar
mvn -q package -pl mcp -DskipTests -Dmcp.transport=stdio  # mcp-2.0.2-SNAPSHOT-stdio.jar
set STDIO_JAR mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar
set HTTP_JAR mcp/target/mcp-2.0.2-SNAPSHOT-http.jar
set INIT '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"t","version":"1"}}}'
```

Check which transport is packaged in each jar:
```shell
unzip -l $STDIO_JAR | grep -io "io/quarkiverse/mcp/server/[a-z]*/" | sort -u
# io/quarkiverse/mcp/server/runtime/
# io/quarkiverse/mcp/server/stdio/

unzip -l $HTTP_JAR | grep -io "io/quarkiverse/mcp/server/[a-z]*/" | sort -u
# io/quarkiverse/mcp/server/http/
# io/quarkiverse/mcp/server/runtime/
# io/quarkiverse/mcp/server/sse/
```

The `Installed features` log line at startup shows the transport too: `[cdi, mcp-server-stdio, ...]` or `[cdi, mcp-server-http, ...]`.

## Dev mode: both transports

`mvn quarkus:dev` uses the same profiles: without the property `mcp.transport`, the default profile `all` starts both transports:
```shell
mvn quarkus:dev -pl mcp
# Installed features: [cdi, mcp-server-http, mcp-server-stdio, smallrye-context-propagation, vertx]
# Listening on: http://localhost:8080
```

The HTTP endpoint `http://localhost:8080/mcp` (and the Dev UI) is then usable as usual. To also use the stdio transport, stdout must only contain JSON-RPC messages: in dev mode the logs of the app, the Maven output and the debugger line (`Listening for transport dt_socket at address: 5005`) are all written to stdout. Disable them:
```shell
begin
    sleep 35   # wait for the dev mode to start
    printf '%s\n' $INIT '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
    sleep 5
end | mvn -q quarkus:dev -pl mcp \
        -Ddebug=false -Dquarkus.console.enabled=false -Dquarkus.log.console.enabled=false
# {"jsonrpc":"2.0","id":1,"result":{"capabilities":{...}}}
# {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"quarkit_analyze",...
```

Notes:
- The logs are still available in the file `mcp/quarkus-mcp-mtool.log`.
- When stdin is closed, the app stops (`mcp stopped in 0.023s`) but the dev mode process keeps running: stop it with `Ctrl+C` or `pkill -f quarkus:dev`.

## stdio

Send the `initialize` request, the `initialized` notification and `tools/list` on stdin. The `sleep` keeps stdin open long enough to get the responses (the server stops when stdin is closed).

Optional: occupy the port `8080` with any process (here a Python HTTP server) to verify that the server doesn't try to bind it:
```shell
python3 -m http.server 8080 >/dev/null 2>&1 &
set BLOCKER $last_pid
```

```shell
begin
    printf '%s\n' $INIT \
        '{"jsonrpc":"2.0","method":"notifications/initialized"}' \
        '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
    sleep 8
end | java -jar $STDIO_JAR 2>/tmp/mcp-stdio.err

# {"jsonrpc":"2.0","id":1,"result":{"capabilities":{...},"serverInfo":{"name":"mcp","version":"2.0.2-SNAPSHOT","title":"mcp"},...}}
# {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"quarkit_analyze",...

grep -o "Installed features.*\|Listening on:.*" /tmp/mcp-stdio.err
# Installed features: [cdi, mcp-server-stdio, smallrye-context-propagation, vertx]   <- no "Listening on:", no HTTP port

kill $BLOCKER   # if the optional port blocker was started
```

Configure the `acp` client to launch the server in stdio mode:
```shell
--mcp-server-config '{"type":"stdio","name":"mtools","command":"java","args":["-jar","./mcp/target/mcp-2.0.2-SNAPSHOT-stdio.jar"]}'
--mcp-server-config '{"type":"stdio","name":"mtools","command":"jbang","args":["mcp@quarkit"]}'
```

## HTTP

Start the HTTP server (it stays up even if stdin is closed):
```shell
java -jar $HTTP_JAR </dev/null &
```

Query it with curl. The Streamable HTTP transport returns a `Mcp-Session-Id` header from `initialize` which must be sent with the next requests (without it, `tools/list` returns an empty response):
```shell
set H -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream'

# 1. initialize and extract the session id from the response headers
set SID (curl -s -D - -o /dev/null -X POST localhost:8080/mcp $H -d $INIT \
    | grep -i '^mcp-session-id' | cut -d' ' -f2 | tr -d '\r')

# 2. initialized notification -> HTTP 202
curl -s -X POST localhost:8080/mcp $H -H "Mcp-Session-Id: $SID" \
    -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' -w 'HTTP %{http_code}\n'

# 3. list the tools
curl -s -X POST localhost:8080/mcp $H -H "Mcp-Session-Id: $SID" \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | grep -o '"name":"quarkit_[a-z_]*"'
# "name":"quarkit_analyze"
# "name":"quarkit_reference_lookup"
# "name":"quarkit_references_list"
# "name":"quarkit_validate"

pkill -f "mcp-2.0.2-SNAPSHOT-http.jar"
```

## Pass Quarkus properties with JBang

The `-D` options must be placed **before** the alias, otherwise they are passed to the application as arguments and ignored:

| Command | Result |
|---|---|
| `jbang -Dquarkus.http.port=9090 mcp-http@quarkit` | ✅ applied |
| `jbang -R-Dquarkus.http.port=9090 mcp-http@quarkit` | ✅ applied (`-R` passes any JVM option) |
| `QUARKUS_HTTP_PORT=9090 jbang mcp-http@quarkit` | ✅ applied (environment variable) |
| `jbang mcp-http@quarkit -Dquarkus.http.port=9090` | ❌ ignored |

Check that a property is applied by looking at the `Listening on:` log line:
```shell
jbang -Dquarkus.http.port=18090 mcp-http@quarkit </dev/null >/tmp/jb-alias.log 2>&1 &
sleep 15; grep -oE "mcp [0-9.A-Z-]+ on JVM|Listening on:.*" /tmp/jb-alias.log
# mcp 2.0.2 on JVM
# Listening on: http://0.0.0.0:18090

curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X POST localhost:18090/mcp \
    -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -d $INIT
# HTTP 200
pkill -f "quarkus.http.port=18090"
```
