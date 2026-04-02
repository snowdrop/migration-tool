# AI

Project created to test different model providers with their API

## Google Vertex AI

The jbang `VertexAIAnthropicWithScanner` and `VertexAIAnthropicChat.java` uses the `LangChain4j vertex-ai-anthropic` library to access the Google Cloud platform. 
The protocol used to communicate with the platform is `gRPC` and is managed by the Google Cloud AI SDK. This module of langchain4j relies on the Google Cloud AI platform SDK.

Google documentation: 
- https://docs.cloud.google.com/java/docs/reference/google-cloud-aiplatform/latest/overview
- https://github.com/googleapis/google-cloud-java/tree/main/java-aiplatform#quickstart

## Quarkus Langchain4j - Vertex AI Models

### Quarkus dev

You can play with the new module: `Quarkus Langchain4j - Vertex AI Models` using the Picocli `PoemCommand` command and the `MyAiservice` that quarkus
CDI will register as Langchain4j AiServices.
```shell
mvn quarkus:dev
```

### jbang

The jbang `QuarkusVertexAIModels` is a Quarkus & Picocli example able to issue a query against the Google Cloud Vertex API (model gardens)
using the new `Quarkus Vertex AI Models` module based on the QuarkusRestclient and accessing the Google API using the `rawPredict` endpoint:
https://docs.cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.endpoints/rawPredict

```shell
❯ jbang run ./src/main/java/QuarkusVertexAIModels.java
[jbang] Building jar for QuarkusVertexAIModels.java...
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
2026-03-25 15:21:58,177 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.models.VertexAiBaseChatModel] (main) Request: [UserMessage { name = null, contents = [TextContent { text = "Hi Claude" }], attributes = {} }]                                                                                                                                                                                                
2026-03-25 15:21:58,854 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.models.VertexAiRestApi$VertxAiClientLogger] (vert.x-eventloop-thread-2) Request:                                                 
- method: POST
- url: https://europe-west1-aiplatform.googleapis.com/v1/projects/itpc-gcp-cp-pe-eng-claude/locations/europe-west1/publishers/anthropic/models/claude-opus-4-6:rawPredict
- headers: [Authorization: Bearer ya...09], [Content-Type: application/json], [User-Agent: Quarkus REST Client], [content-length: 110]
- body: {"anthropic_version":"vertex-2023-10-16","max_tokens":1024,"messages":[{"role":"user","content":"Hi Claude"}]}
2026-03-25 15:22:00,903 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.models.VertexAiRestApi$VertxAiClientLogger] (vert.x-eventloop-thread-2) Response:                                                
- status code: 200
- headers: [content-type: application/json], [x-vertex-ai-internal-prediction-backend: harpoon], [date: Wed, 25 Mar 2026 14:22:00 GMT], [request-id: req_vrtx_011CZPwvaXWt3cGD9r2W9LcV], [Vary: X-Origin], [Vary: Referer], [Server: scaffolding on HTTPServer2], [X-XSS-Protection: 0], [X-Frame-Options: SAMEORIGIN], [X-Content-Type-Options: nosniff], [Alt-Svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000], [Accept-Ranges: none], [Vary: Origin,Accept-Encoding], [Transfer-Encoding: chunked]                                                                                                                           
- body: {"model":"claude-opus-4-6","id":"msg_vrtx_01JixAPaKzY8NJ3HE6GPE3ue","type":"message","role":"assistant","content":[{"type":"text","text":"Hi there! How are you doing today? Is there anything I can help you with? 😊"}],"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":9,"cache_creation_input_tokens":0,"cache_read_input_tokens":0,"cache_creation":{"ephemeral_5m_input_tokens":0,"ephemeral_1h_input_tokens":0},"output_tokens":24}}                                                                                                                                                           
2026-03-25 15:22:00,940 INFO  [io.quarkiverse.langchain4j.vertexai.runtime.models.VertexAiBaseChatModel] (main) Response: ChatResponse { aiMessage = AiMessage { text = "Hi there! How are you doing today? Is there anything I can help you with? 😊", thinking = null, toolExecutionRequests = [], attributes = {} }, metadata = ChatResponseMetadata{id='null', modelName='null', tokenUsage=null, finishReason=null} }                                                                                                                                                                                                          
Hi there! How are you doing today? Is there anything I can help you with? 😊                              
```
