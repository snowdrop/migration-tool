# Google Generative & Vertex AI

This project has been created to test different Java LLM libraries to access Anthropic Claude running as partner model on Vertex AI.
the endpoint to be accessed using the Vertex AI API is rawPredict which is documented here: https://docs.cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.endpoints/rawPredict

## AI environment variables

To access the Vertex AI API - Anthropic, you will have to set the following properties:
```shell
export QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION=<GOOGLE_CLOUD_PLATFORM_LOCATION>
export QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID=<GOOGLE_CLOUD_PLATFORM_PROJECT_ID>
```
**Remark**: The model defined by default is `claude-opus-4-6` and can be changed using this variable: `QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID`

## Langchain4j - Vertex AI

The jbang `VertexAIAnthropicWithScanner` and `VertexAIAnthropicChat.java` uses the `LangChain4j vertex-ai-anthropic` library to access the Google Cloud platform. 
The protocol used to communicate with the platform is `gRPC`.  This module of langchain4j relies on the Google Cloud AI platform SDK.

```bash
jbang run ./src/main/java/VertexAIAnthropicChat.java
jbang run ./src/main/java/VertexAIAnthropicWithScanner.java
```

Google documentation: 
- https://docs.cloud.google.com/java/docs/reference/google-cloud-aiplatform/latest/overview
- https://github.com/googleapis/google-cloud-java/tree/main/java-aiplatform#quickstart

## Quarkus Langchain4j - Vertex AI

### Quarkus Application

You can access the Vertex AI API and Anthropic model using the new module: `Quarkus Langchain4j - Vertex AI Models`. The Quarkus application
runs a `CodeAssistantCommand` command, register an AI service able to chat with the model using your request/task provided as picocli argument 
and if needed the model can request to read/write files locally using the registered tools: `readFile` and `writeFile`

To use it, run the following command where you pass the task(s) to be executed
```shell
mvn quarkus:dev -Dquarkus.args="'Read the ./pom.xml file and tell me if it includes as quarkus extension vertex ai'"
```

### jbang script

The jbang `QuarkusVertexAiAnthropic` is also a Quarkus & Picocli example similar to the previous example but where we have coded some
messages
```java
AiMessage aiMessage = new AiMessage("You are a java expert");
UserMessage userMessage = new UserMessage("What is a Java Enum ?");
```

To launch it, execute this command:
```shell
❯ jbang run ./src/main/java/QuarkusVertexAiAnthropic.java
[jbang] Building jar for QuarkusVertexAiAnthropic.java...
[jbang] Post build with io.quarkus.launcher.JBangIntegration
[jbang] Quarkus augmentation completed in 837ms
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2026-03-25 15:21:58,112 INFO  [io.quarkus] (main) quarkus 999-SNAPSHOT on JVM (powered by Quarkus 3.31.3) started in 0.308s. 
2026-03-25 15:21:58,116 INFO  [io.quarkus] (main) Profile prod activated.                                                                                                                                   
2026-03-25 15:21:58,116 INFO  [io.quarkus] (main) Installed features: [cdi, langchain4j, langchain4j-vertexai-models, picocli, qute, rest-client, rest-client-jackson, smallrye-context-propagation, vertx] 
Starting AI ...                                                                                                                                                                                             
2026-03-25 15:21:58,177 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiBaseChatModel] (main) Request: [UserMessage { name = null, contents = [TextContent { text = "Hi Claude" }], attributes = {} }]                                                                                                                                                                                                
2026-03-25 15:21:58,854 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiRestApi$VertxAiClientLogger] (vert.x-eventloop-thread-2) Request:                                                 
- method: POST
- url: https://europe-west1-aiplatform.googleapis.com/v1/projects/itpc-gcp-cp-pe-eng-claude/locations/europe-west1/publishers/anthropic/models/claude-opus-4-6:rawPredict
- headers: [Authorization: Bearer ya...09], [Content-Type: application/json], [User-Agent: Quarkus REST Client], [content-length: 110]
- body: {"anthropic_version":"vertex-2023-10-16","max_tokens":1024,"messages":[{"role":"user","content":"Hi Claude"}]}
2026-03-25 15:22:00,903 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiRestApi$VertxAiClientLogger] (vert.x-eventloop-thread-2) Response:                                                
- status code: 200
- headers: [content-type: application/json], [x-vertex-ai-internal-prediction-backend: harpoon], [date: Wed, 25 Mar 2026 14:22:00 GMT], [request-id: req_vrtx_011CZPwvaXWt3cGD9r2W9LcV], [Vary: X-Origin], [Vary: Referer], [Server: scaffolding on HTTPServer2], [X-XSS-Protection: 0], [X-Frame-Options: SAMEORIGIN], [X-Content-Type-Options: nosniff], [Alt-Svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000], [Accept-Ranges: none], [Vary: Origin,Accept-Encoding], [Transfer-Encoding: chunked]                                                                                                                           
- body: {"model":"claude-opus-4-6","id":"msg_vrtx_01JixAPaKzY8NJ3HE6GPE3ue","type":"message","role":"assistant","content":[{"type":"text","text":"Hi there! How are you doing today? Is there anything I can help you with? 😊"}],"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":9,"cache_creation_input_tokens":0,"cache_read_input_tokens":0,"cache_creation":{"ephemeral_5m_input_tokens":0,"ephemeral_1h_input_tokens":0},"output_tokens":24}}                                                                                                                                                           
2026-03-25 15:22:00,940 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiBaseChatModel] (main) Response: ChatResponse { aiMessage = AiMessage { text = "Hi there! How are you doing today? Is there anything I can help you with? 😊", thinking = null, toolExecutionRequests = [], attributes = {} }, metadata = ChatResponseMetadata{id='null', modelName='null', tokenUsage=null, finishReason=null} }                                                                                                                                                                                                          
Hi there! How are you doing today? Is there anything I can help you with? 😊                              
```

## Example of curl requests

You can curl the Vertex AI API and endpoint [rawPredict](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.endpoints/rawPredict) using curl with the following payload and headers:

```bash
export PROJECT_ID=<GOOGLE_CLOUD_PLATFORM_PROJECT_ID>
export LOCATION=<GOOGLE_CLOUD_PLATFORM_LOCATION>
export MODEL_ID=claude-opus-4-6
export MAX_TOKENS=1024
export ANTHROPIC_VERSION=vertex-2023-10-16

JSON_BODY=$(cat <<EOF
{
"anthropic_version": "$ANTHROPIC_VERSION",
"messages": [
{ "role": "user", "content": "What is a java record" },
{ "role": "user", "content": "and also a Java enum ?" }
],
"max_tokens": $MAX_TOKENS
}
EOF
)

curl -sX POST \
-H "Authorization: Bearer $(gcloud auth print-access-token)" \
-H "Content-Type: application/json; charset=utf-8" \
-d "$JSON_BODY" \
"https://$LOCATION-aiplatform.googleapis.com/v1/projects/$PROJECT_ID/locations/$LOCATION/publishers/anthropic/models/$MODEL_ID:rawPredict" | jq
```