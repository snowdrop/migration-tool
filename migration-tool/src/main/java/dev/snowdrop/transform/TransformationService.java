package dev.snowdrop.transform;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.transform.provider.MigrationProvider;
import dev.snowdrop.transform.provider.ProviderFactory;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for orchestrating transformation execution across different providers.
 */
public class TransformationService {

    private static final Logger logger = Logger.getLogger(TransformationService.class);

    /**
     * Executes transformation for a migration task using appropriate providers.
     *
     * @param task the migration task to execute
     * @param context the execution context
     * @return aggregated execution result from all providers
     */
    public ExecutionResult executeTransformation(MigrationTask task, ExecutionContext context) {
        var rule = task.getRule();

        if (rule.instructions() == null) {
            return ExecutionResult.failure("No instructions found for task: " + rule.ruleID());
        }

        List<String> allDetails = new ArrayList<>();
        boolean overallSuccess = true;

        logger.infof("🔄 Processing migration task: %s", rule.ruleID());
        if (context.verbose()) {
            logger.infof("   Description: %s", rule.description());
            logger.infof("   Category: %s", rule.category());
            logger.infof("   Effort: %d", rule.effort());
        }

        // Execute OpenRewrite instructions
        if (rule.instructions().openrewrite() != null) {
            ExecutionResult result = executeProvider("openrewrite", task, context);
            allDetails.addAll(result.details());
            if (!result.success()) {
                overallSuccess = false;
                logger.errorf("   ❌ OpenRewrite execution failed: %s", result.message());
            } else {
                logger.infof("   ✅ OpenRewrite execution completed successfully");
            }
        }

        // Execute AI instructions
        if (rule.instructions().ai() != null) {
            ExecutionResult result = executeProvider("ai", task, context);
            allDetails.addAll(result.details());
            if (!result.success()) {
                overallSuccess = false;
                logger.errorf("   ❌ AI execution failed: %s", result.message());
            } else {
                logger.infof("   ✅ AI execution completed");
            }
        }

        // Execute Manual instructions
        if (rule.instructions().manual() != null) {
            ExecutionResult result = executeProvider("manual", task, context);
            allDetails.addAll(result.details());
            if (!result.success()) {
                overallSuccess = false;
                logger.errorf("   ❌ Manual instruction processing failed: %s", result.message());
            } else {
                logger.infof("   ✅ Manual instructions processed");
            }
        }

        if (overallSuccess) {
            return ExecutionResult.success("All transformations completed successfully", allDetails);
        } else {
            return ExecutionResult.failure("Some transformations failed", allDetails, null);
        }
    }

    private ExecutionResult executeProvider(String providerType, MigrationTask task, ExecutionContext context) {
        return ProviderFactory.getProvider(providerType)
            .map(provider -> {
                logger.infof("   Executing %s provider...", providerType);
                return provider.execute(task, context);
            })
            .orElse(ExecutionResult.failure("Provider not found: " + providerType));
    }
}