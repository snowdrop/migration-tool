package dev.snowdrop.ai; /// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.13.0

//DEPS dev.langchain4j:langchain4j-agentic:1.13.0-beta23
//DEPS dev.langchain4j:langchain4j-vertex-ai-anthropic:1.13.0-beta23
//DEPS com.opencsv:opencsv:5.7.1
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=info

import com.opencsv.CSVReader;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.vertexai.anthropic.VertexAiAnthropicChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class JBangMigrationAiTool {

    static class MigrationActions {
        @Tool("Execute the Maven OpenRewrite goal to apply the changes")
        public String executeMavenRewrite(String recipeName) {
            try {
                // Mvn rewrite command
                String command = String.format("mvn rewrite:run -Drewrite.activeRecipes=%s", recipeName);

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String output = reader.lines().limit(20).collect(Collectors.joining("\n"));
                int exitCode = process.waitFor();

                return "Command executed. Exit code : " + exitCode + "\nLog extract :\n" + output;
            } catch (Exception e) {
                return "Command failed : " + e.getMessage();
            }
        }

        @Tool("Read the openrewrite CSV file")
        public String analyzeCSV(String filePath) {
            try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
                List<String[]> lines = reader.readAll();
                return lines.stream()
                        .limit(50)
                        .map(line -> String.join(" | ", line))
                        .collect(Collectors.joining("\n"));
            } catch (Exception e) {
                return "Reading CSV file error: " + e.getMessage();
            }
        }

        @Tool("Generate an OpenRewrite YAML recipe able to change the java method name of a class")
        public String generateRewriteYaml(String recipeName, String description, String searchPattern, String replacePattern) {
            return String.format("""
                    ---
                    type: specs.openrewrite.org/v1beta/recipe
                    name: %s
                    displayName: %s
                    recipeList:
                      - org.openrewrite.java.ChangeMethodName:
                          methodPattern: %s
                          newMethodName: %s
                    """, recipeName, description, searchPattern, replacePattern);
        }
    }

    interface MigrationAssistant {
        String process(String text);
    }

    public static void main(String[] args) {
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        System.out.println("==== Starting to connect to LLM ====");
        ChatModel aiModel = VertexAiAnthropicChatModel.builder().project(projectId).location(location)
                .modelName(modelId).maxTokens(1000).logRequests(true).logResponses(true).build();

        MigrationAssistant assistant = AiServices.builder(MigrationAssistant.class)
                .chatModel(aiModel)
                .tools(new MigrationActions())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // Example of flow for AI
        String instruction = "Analyse 'rewrite-report.csv'. If you see a spring security issue, " +
                "apply the recipe 'org.openrewrite.java.spring.security.ChangeSpringSecurity' using Maven.";

        System.out.println("--- Agent launch ---");
        System.out.println(assistant.process(instruction));
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
