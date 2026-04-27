///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-picocli:3.31.3
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-core:999-SNAPSHOT
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-skills:999-SNAPSHOT
//DEPS dev.snowdrop.mtool.ai:vertex-ai-anthropic:1.0.5-SNAPSHOT
//SOURCES ScannerTool.java

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiAnthropicChatModel;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Duration;

@CommandLine.Command
public class SkillsQuarkusAgent implements Runnable {

    @CommandLine.Option(
            names = {"-s", "--skill"},
            description = "Path directory of the skill to be executed",
            required = true
    )
    String skillsPath;

    @Override
    public void run() {
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");
        String publisher = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PUBLISHER", "anthropic");
        int duration = Integer.parseInt(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_DURATION", "300"));
        int maxTokens = Integer.parseInt(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MAX_TOKENS", "1000"));
        boolean logRequests = Boolean.parseBoolean(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOG_REQUESTS", "false"));
        boolean logResponses = Boolean.parseBoolean(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOG_RESPONSES", "false"));

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        ChatModel model = VertexAiAnthropicChatModel.builder()
                .projectId(projectId)
                .location(location)
                .modelId(modelId)
                .publisher(publisher)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofSeconds(duration))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .logCurl(false)
                .build();

        Skills skills = Skills.from(FileSystemSkillLoader.loadSkill(Path.of(skillsPath)));

        Assistant agent = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(new ScannerTool())
                .maxSequentialToolsInvocations(100)
                .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                        + "\nWhen the user's request relates to one of these skills, activate it first.")
                .build();

        System.out.println("=== Migration Agent Online ===");
        String response = agent.chat("Migrate Spring Boot applications to Quarkus. Use the automated_responses.yml to get answers");
        System.out.println("Agent: " + response);
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

    interface Assistant {
        String chat(String message);
    }
}