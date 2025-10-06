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
 * Provider implementation for manual transformations.
 * Outputs TODO items for manual developer action.
 */
public class ManualProvider implements MigrationProvider {

    private static final Logger logger = Logger.getLogger(ManualProvider.class);

    @Override
    public String getProviderType() {
        return "manual";
    }

    @Override
    public ExecutionResult execute(MigrationTask task, ExecutionContext context) {
        var rule = task.getRule();

        if (rule.instructions() == null || rule.instructions().manual() == null) {
            return ExecutionResult.failure("No manual instructions found for task");
        }

        List<String> details = new ArrayList<>();

        logger.infof("📋 Manual actions required for task: %s", task.getRule().ruleID());

        for (Rule.Manual manualInstruction : rule.instructions().manual()) {
            String todo = manualInstruction.todo();

            logger.info("------------------------------------------------------------------------------------------");
            logger.infof("   ⚠️  TODO: %s", todo);
            logger.info("------------------------------------------------------------------------------------------");
            details.add("TODO: " + todo);
        }

        logger.infof("   ℹ️  Please complete the above manual tasks before proceeding.");

        return ExecutionResult.success("Manual tasks logged for developer action", details);
    }
}