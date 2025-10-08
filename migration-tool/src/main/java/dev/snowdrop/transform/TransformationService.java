package dev.snowdrop.transform;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.transform.provider.ProviderFactory;
import dev.snowdrop.transform.provider.ai.Assistant;
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
     * Log the execution result. If verbose is enabled, log the details
     *
     * @param result  the detailed result of the command executed during the transformation
     * @param verbose if the details should be logged
     */
    public void logExecutionResult(ExecutionResult result, boolean verbose) {
        if (result.success()) {
            logger.infof("✅ Task completed successfully: %s", result.message());
        } else {
            logger.errorf("❌ Task failed: %s", result.message());
            if (result.exception() != null && verbose) {
                result.exception().printStackTrace();
            }
        }

        if (verbose && !result.details().isEmpty()) {
            logger.info("   Details:");
            for (String detail : result.details()) {
                logger.infof("     - %s", detail);
            }
        }
    }

    /**
     * Executes transformation for a migration task using the appropriate provider.
     *
     * @param task the migration task to execute
     * @param ctx  the execution ctx
     * @return aggregated execution result from all providers
     */
    public ExecutionResult execute(MigrationTask task, ExecutionContext ctx) {

        List<String> allDetails = new ArrayList<>();

        ExecutionResult result = callProvider(ctx.provideType(), task, ctx);
        allDetails.addAll(result.details());

        if (result.success()) {
            return ExecutionResult.success(String.format("   ✅ %s execution completed successfully", ctx.provideType()), allDetails);
        } else {
            return ExecutionResult.failure(String.format("   ❌ %s execution failed !", ctx.provideType()));
        }
    }

    private ExecutionResult callProvider(String providerType, MigrationTask task, ExecutionContext context) {
        return ProviderFactory.getProvider(providerType)
            .map(provider -> {
                return provider.execute(task, context);
            }).orElse(ExecutionResult.failure("Provider not found: " + providerType));
    }
}