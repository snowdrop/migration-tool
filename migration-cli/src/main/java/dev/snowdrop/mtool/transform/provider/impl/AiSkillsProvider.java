package dev.snowdrop.mtool.transform.provider.impl;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.transform.provider.MigrationProvider;
import dev.snowdrop.mtool.transform.provider.ai.SkillsAssistant;
import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;
import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiAnthropicChatModel;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiSkillsProvider implements MigrationProvider {
    private static final Logger logger = Logger.getLogger(AiSkillsProvider.class);
    private ChatModel model;

    public AiSkillsProvider() {
        String projectId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID", "dummy");
        String location = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION", "dummy");
        String modelId = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID", "claude-opus-4-6");
        String publisher = getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PUBLISHER", "anthropic");
        int duration = Integer.parseInt(getEnv("QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_DURATION", "30"));

        validateRequired(projectId, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
        validateRequired(location, "QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");

        model = VertexAiAnthropicChatModel.builder()
                .projectId(projectId)
                .location(location)
                .modelId(modelId)
                .publisher(publisher)
                .maxOutputTokens(20000)
                .timeout(Duration.ofSeconds(duration))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override
    public String getProviderType() {
        return "ai-skills";
    }

    @Override
    public ExecutionResult execute(MigrationTask task, ExecutionContext context) {

        // We only process until now only one instruction/rule !
        var ai = Arrays.stream(task.getRule().instructions().ai()).findFirst().orElse(null);

        if (ai.skills() == null || ai.skills().isEmpty()) {
            return ExecutionResult.failure("No tasks defined with AI instruction, skipping", null);
        }

        try {
            ExecutionResult result = executeAiInstruction(ai, task.getRule(), context);

            if (!result.success()) {
                return ExecutionResult.failure(result.message(), result.details(), null);
            }

            return ExecutionResult.success("AI SKILL execution completed successfully", result.details());
        } catch (Exception e) {
            logger.errorf("Error executing AI SKILL: %s", e.getMessage());
            if (context.verbose()) {
                e.printStackTrace();
            }
            return ExecutionResult.failure("Error executing AI SKILL !", e);
        }
    }

    private ExecutionResult executeAiInstruction(Rule.Ai ai, Rule rule, ExecutionContext context) {
        List<String> details = new ArrayList<>();

        details.add("Processing rule : " + "" + rule.ruleID());

        var skills = ai.skills();

        if (skills == null || skills.isEmpty()) {
            return ExecutionResult.failure(
                    String.format("No AI SKILL defined for rule %s, skipping", rule.ruleID()), null);
        }

        // Get the list of SKILL
        skills.forEach(skill -> {
            logger.infof("Skill: %s", skill);
            details.add("- " + skill);
        });

        boolean success = execAiSkillCmd(context, skills);
        details.add("AI SKILL cmd executed successfully");

        if (success) {
            return ExecutionResult.success("AI instruction executed successfully", details);
        } else {
            return ExecutionResult.failure("AI's chat command execution failed", details, null);
        }
    }

    private boolean execAiSkillCmd(ExecutionContext ctx, List<String> skills) {
        logger.info("Hello! I'm your AI SKILL migration assistant and I will help you moving your code");

        // TODO: Have a property to set the SKILL Agent home folder
        // Claude: .claude/skills

        skills.forEach(s -> {
            logger.infof("============= Sending user message to the model");
            Skills agentSkill = Skills.from(FileSystemSkillLoader.loadSkill(Path.of(ctx.aiSkillsHomeDir(), s)));
            SkillsAssistant service = AiServices.builder(SkillsAssistant.class)
                    .chatModel(model)
                    .systemMessage(agentSkill.formatAvailableSkills())
                    .build();

            String response = service.chat("Migrate the application using the SKILL provided");
            logger.infof("============= Model response: %s", response);
        });

        return true;
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
