package dev.snowdrop.transform.provider.impl;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.transform.provider.MigrationProvider;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider implementation for AI-based transformations.
 * Currently, a stub implementation for future AI integration.
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
            return ExecutionResult.failure("No AI instructions found for task");
        }

        List<String> details = new ArrayList<>();

        for (Rule.Ai aiInstruction : rule.instructions().ai()) {
            String promptMessage = aiInstruction.promptMessage();

            logger.info("------------------------------------------------------------------------------------------");
            logger.infof("AI Instruction: %s", promptMessage);
            logger.info("------------------------------------------------------------------------------------------");
            details.add("AI prompt: " + promptMessage);

            // TODO: Integrate with AI service to process the prompt
            // For now, this is a placeholder implementation
            details.add("AI processing not yet implemented");
        }

        logger.warnf("AI provider is not fully implemented yet. Instructions logged for manual review.");

        return ExecutionResult.success("AI instructions logged (implementation pending)", details);
    }
}