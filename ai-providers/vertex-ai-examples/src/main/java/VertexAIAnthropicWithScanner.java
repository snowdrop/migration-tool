/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.12.2
//DEPS dev.langchain4j:langchain4j-anthropic:1.12.2
//DEPS dev.langchain4j:langchain4j-vertex-ai-anthropic:1.12.2-beta22
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=debug

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.vertexai.anthropic.VertexAiAnthropicChatModel;

import java.util.Scanner;

public class VertexAIAnthropicWithScanner {

    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        ChatModel model = VertexAiAnthropicChatModel.builder().project(projectId).location(location)
                .modelName(modelId).maxTokens(1000).logRequests(true).logResponses(true).build();

        Assistant assistant = AiServices.builder(Assistant.class).chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)).build();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Chat started! Type 'exit' to quit.");

        while (true) {
            System.out.print("User: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input))
                break;

            String response = assistant.chat(input);
            System.out.println("AI: " + response);
        }
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