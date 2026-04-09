///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.13.0
//DEPS dev.langchain4j:langchain4j-agentic:1.13.0-beta23
//DEPS dev.langchain4j:langchain4j-vertex-ai-anthropic:1.13.0-beta23
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=info

package dev.snowdrop.ai;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.observability.MonitoredAgent;
import dev.langchain4j.agentic.observability.MonitoredExecution;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.model.vertexai.anthropic.VertexAiAnthropicChatModel;

import java.nio.file.Path;

public class JBangAiFlow {
    public static void main(String[] args) {
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        System.out.println("==== Starting to connect to LLM ====");
        ChatModel aiModel = VertexAiAnthropicChatModel.builder().project(projectId).location(location)
                .modelName(modelId).maxTokens(1000).logRequests(true).logResponses(true).build();

        TaskCategoryRouter routerAgent = AgenticServices.agentBuilder(TaskCategoryRouter.class)
                .chatModel(aiModel)
                .outputKey("category")
                .build();

        JavaCodingAssistant javaCodingAssistant = AgenticServices
                .agentBuilder(JavaCodingAssistant.class)
                .chatModel(aiModel)
                .outputKey("response").build();

        UntypedAgent tasksAgent = AgenticServices.conditionalBuilder()
                .subAgents(
                        "category is AI",
                        agenticScope ->
                                agenticScope.readState("category", TaskCategory.UNKNOWN) == TaskCategory.AI,
                        javaCodingAssistant)
                .subAgents(
                        "category is REWRITE",
                        agenticScope ->
                                agenticScope.readState("category", TaskCategory.UNKNOWN) == TaskCategory.REWRITE,
                        new ExecuteRecipeFromTask())
                .build();

        var aFlow = AgenticServices.sequenceBuilder(TasksRouterAgentInstance.class)
                .subAgents(routerAgent, tasksAgent)
                .outputKey("response")
                .build();

        System.out.println(aFlow.ask("I have an OpenRewrite recipe to be executed: verifyJavaCode"));
        System.out.println(aFlow.ask("Can you, as Java Coding assistant AI expert, tell me what a Java enum is in maximum 2 lines ?"));

        System.out.println("==== Report monitoring data ====");
        AgentMonitor agentMonitor = aFlow.agentMonitor();
        MonitoredExecution execution = agentMonitor.successfulExecutions().get(0);
        System.out.println(execution);

        System.out.println("==== Generate the monitoring data report ====");
        HtmlReportGenerator.generateReport(agentMonitor, Path.of("non-ai-agents.html"));

    }

    public interface TasksRouterAgentInstance extends MonitoredAgent {
        @Agent
        String ask(@V("request") String request);
    }

    public interface TaskCategoryRouter extends MonitoredAgent {
        @UserMessage("""
            Analyze the user request and categorize it as 'ai' or 'rewrite',
            In case the request doesn't belong to any of those categories categorize it as 'unknown'.
            Reply with only one of those words and nothing else.

            The user request is: '{{request}}'.
            """)
        @Agent(description = "Categorize a user request tasks", outputKey = "category")
        TaskCategory askToAgent(@V("request") String request);
    }

    public enum TaskCategory {
        AI, REWRITE, MANUAL, UNKNOWN
    }

    public static class ExecuteRecipeFromTask {
        @Agent(description = "Execute an OpenRewrite recipe using as input the FQName of the recipe java class", outputKey = "response")
        public static String executeTask(@V("request") String recipeFQName) {
            return String.format("OpenRewrite task executed: %s",recipeFQName);
        }
    }

    public interface JavaCodingAssistant extends MonitoredAgent {
        @UserMessage("""
            You are a AI Java Coding assistant.
            Analyze the following user request and provide the best coding response.
            The user request is {{request}}.
            """)
        @Agent(description = "An AI Java coding assistant", outputKey = "response")
        String toCode(@V("request") String request);
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
