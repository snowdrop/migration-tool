package dev.snowdrop.transform.provider.impl;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.transform.provider.MigrationProvider;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;
import org.jboss.logging.Logger;

import java.util.Arrays;

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
        return null;
    }
}