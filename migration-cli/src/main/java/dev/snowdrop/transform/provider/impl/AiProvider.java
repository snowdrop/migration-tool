package dev.snowdrop.transform.provider.impl;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.transform.provider.MigrationProvider;
import dev.snowdrop.transform.provider.ai.Assistant;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider implementation for AI-based transformations.
 */
public class AiProvider implements MigrationProvider {

    private static final Logger logger = Logger.getLogger(AiProvider.class);

    @Override
    public String getProviderType() {
        return "ai";
    }

    @Override
    public ExecutionResult execute(MigrationTask task, ExecutionContext context) {
        var rule = task.getRule();

        if (rule.instructions() == null || rule.instructions().ai() == null) {
            return ExecutionResult.failure(String.format("No AI instructions found for rule: %s", rule.ruleID()));
        }

        // We only process until now only one openrewrite object/rule !!
        var ai = Arrays.stream(rule.instructions().ai()).findFirst().orElse(null);

        if (ai.tasks() == null || ai.tasks().isEmpty()) {
            return ExecutionResult.failure("No tasks defined in AI instruction, skipping", null);
        }

        try {
            ExecutionResult result = executeAiInstruction(ai, rule, context);

            if (!result.success()) {
                return ExecutionResult.failure(result.message(), result.details(), null);
            }

            return ExecutionResult.success("AI execution completed successfully", result.details());
        } catch (Exception e) {
            logger.errorf("Error executing AI instruction: %s", e.getMessage());
            if (context.verbose()) {
                e.printStackTrace();
            }
            return ExecutionResult.failure("Error executing AI instruction !", e);
        }

    }

    private ExecutionResult executeAiInstruction(Rule.Ai ai, Rule rule, ExecutionContext context) {
        List<String> details = new ArrayList<>();

        details.add("Processing rule : " + "" + rule.ruleID());

        var tasks = ai.tasks();

        if (tasks == null || tasks.isEmpty()) {
            return ExecutionResult.failure(
                    String.format("No tasks defined in AI instruction for rule %s, skipping", rule.ruleID()), null);
        }

        // Get the list of instructions / rule
        ai.tasks().forEach(task -> {
            logger.infof("Task: %s", task);
            details.add("- " + task);
        });

        boolean success = execAiCmd(context, tasks, details);
        details.add("Ai cmd executed successfully");

        if (success) {
            return ExecutionResult.success("AI instruction executed successfully", details);
        } else {
            return ExecutionResult.failure("AI's chat command execution failed", details, null);
        }
    }

    private boolean execAiCmd(ExecutionContext ctx, List<String> tasks, List<String> details) {
        logger.info("Hello! I'm your AI migration assistant.");

        Console console = System.console();

        // TODO: To be improved
        if (console == null) {
            details.add("Java console not available.");
            return false;
        }

        tasks.forEach(t -> {
            logger.infof("============= Sending user message");
            String response = ctx.assistant().chat(t);
            logger.infof("============= Claude response: %s", response);
        });

        return true;
    }
}