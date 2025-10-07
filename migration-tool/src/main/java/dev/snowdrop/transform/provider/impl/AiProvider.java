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

        List<String> tasks = new ArrayList<>();

        logger.info("------------------------------------------------------------------------------------------");
        for (Rule.Ai aiInstruction : rule.instructions().ai()) {
            tasks = aiInstruction.tasks();
            tasks.forEach(t -> {
                logger.infof("- %s", t);
            });

        }
        logger.info("------------------------------------------------------------------------------------------");

        return ExecutionResult.success("Executing the AI tasks ...", tasks);
    }
}