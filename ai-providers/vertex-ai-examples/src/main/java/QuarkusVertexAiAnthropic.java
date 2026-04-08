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
        String publisher = "anthropic";
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        System.out.println("Starting AI ...");
        VertexAiAnthropicChatModel chatModel = VertexAiAnthropicChatModel.builder().projectId(projectId).location(location)
                .modelId(modelId).publisher(publisher).logRequests(true).logResponses(true).maxOutputTokens(1024)
                .timeout(Duration.ofSeconds(30)).build();

        AiMessage aiMessage = new AiMessage("You are a java expert");
        UserMessage userMessage = new UserMessage("What is a Java Enum ?");

        System.out.println(chatModel.chat(aiMessage, userMessage));
    }

    /**
     * Helper to get Env Var or return a default.
     */
    private static String getEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    /**
     * Helper to enforce required fields.
     */
    private static void validateRequired(String value, String envName) {
        if (value == null || value.isBlank() || value.equals("dummy")) {
            throw new IllegalStateException(
                    "CRITICAL ERROR: The environment variable '" + envName + "' is required but not set.");
        }
    }
}
