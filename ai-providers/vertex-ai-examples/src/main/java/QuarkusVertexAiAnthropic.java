///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-picocli:3.31.3
//DEPS dev.snowdrop.mtool.ai:vertex-ai-anthropic:1.0.5-SNAPSHOT

import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiAnthropicChatModel;
import picocli.CommandLine;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.Duration;

@CommandLine.Command
public class QuarkusVertexAiAnthropic implements Runnable {
    private static final String PROJECT_ID = "itpc-gcp-cp-pe-eng-claude";
    private static final String MODEL_NAME = "claude-opus-4-6";
    private static final String LOCATION = "europe-west1";
    private static final String PUBLISHER = "anthropic";

    /*
     * "anthropic_version": "vertex-2023-10-16", <========
     * "messages": [
     * {
     * "role": "user",
     * "content": "Hi Claude. Can you tell me what is Quarkus"
     * }
     * ],
     * "max_tokens": 1024
     * }' \
     * "https://europe-west1-aiplatform.googleapis.com/v1/projects/itpc-gcp-cp-pe-eng-claude/locations/europe-west1/publishers/anthropic/models/$MODEL_ID:rawPredict"
     * "https://europe-west1-aiplatform.googleapis.com/v1/projects/itpc-gcp-cp-pe-eng-claude/locations/europe-west1/publishers/anthropic/models/claude-opus-4-6:rawPredict"
     */
    @Override
    public void run() {
        System.out.println("Starting AI ...");
        VertexAiAnthropicChatModel chatModel = VertexAiAnthropicChatModel.builder().projectId(PROJECT_ID).location(LOCATION)
                .modelId(MODEL_NAME).publisher(PUBLISHER).logRequests(true).logResponses(true).maxOutputTokens(1024)
                .timeout(Duration.ofSeconds(30)).build();

        AiMessage aiMessage = new AiMessage("You are a java expert");
        UserMessage userMessage = new UserMessage("What is a Java Enum ?");

        System.out.println(chatModel.chat(aiMessage, userMessage));
    }
}
