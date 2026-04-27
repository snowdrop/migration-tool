
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.13.1
//DEPS dev.langchain4j:langchain4j-anthropic:1.13.1
//DEPS dev.langchain4j:langchain4j-skills:1.13.0-beta23
//DEPS dev.snowdrop.mtool.ai:vertex-ai-anthropic:1.0.5-SNAPSHOT
////DEPS dev.langchain4j:langchain4j-vertex-ai-anthropic:1.13.1-beta23
//SOURCES Assistant.java
//SOURCES ScannerTool.java
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=debug

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
//import dev.langchain4j.model.vertexai.anthropic.VertexAiAnthropicChatModel;
import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiAnthropicChatModel;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Scanner;
import java.util.logging.Logger;

public class SkillsAgent {

    private static Logger logger = Logger.getLogger(SkillsAgent.class.getName());

    public static void main(String[] args) {
        /*
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        ChatModel model = VertexAiAnthropicChatModel.builder()
                .project(projectId)
                .location(location)
                .modelName(modelId)
                .maxTokens(100000)
                .logRequests(true)
                .logResponses(true)
                .build();
         */

        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");
        String publisher = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PUBLISHER", "anthropic");
        int duration = Integer.parseInt(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_DURATION", "30"));
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

        String skillsPath = (args.length > 0 && !args[0].isBlank()) ? args[0] : getEnv("SKILLS_PATH", null);
        if (skillsPath == null || skillsPath.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL ERROR: Skills path must be provided as the first argument or via the 'SKILLS_PATH' environment variable.");
        }

        Skills skills = Skills.from(FileSystemSkillLoader.loadSkill(Path.of(skillsPath)));

        Assistant agent = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(new ScannerTool())
                .toolProvider(skills.toolProvider())
                .maxSequentialToolsInvocations(100)
                .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                        + "\nWhen the user's request relates to one of these skills, activate it first.")
                .build();

        Scanner scanner = new Scanner(System.in);
        // Migrate Spring Boot application to Quarkus. Use the automated_responses.yml to get answers using the match_query and answers. The question/answer are defined in the YAML file under intent_responses
        logger.info("=== Migration Agent Online ===");
        while (true) {
            System.out.print("User: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input))
                break;

            String response = agent.chat(input);
            System.out.println("AI: " + response);
        }


        //System.out.println("=== Migration Agent Online ===");
        //String response = agent.chat("Migrate Spring Boot application to Quarkus. Use the automated_responses.yml to get answers using the match_query and answer within this file");
        //logger.info("Agent: " + response);
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