package dev.snowdrop.transform;

import dev.snowdrop.analyze.model.MigrationTask;
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
     * Executes transformation for a migration task using the appropriate provider.
     *
     * @param task    the migration task to execute
     * @param ctx the execution ctx
     * @return aggregated execution result from all providers
     */
    public ExecutionResult executeTransformation(MigrationTask task, ExecutionContext ctx) {

        List<String> allDetails = new ArrayList<>();

        ExecutionResult result = executeProvider(ctx.provideType(), task, ctx);
        allDetails.addAll(result.details());

        if (result.success()) {
            return ExecutionResult.success(String.format("   ✅ %s execution completed successfully",ctx.provideType()), allDetails);
        } else {
            return ExecutionResult.failure(String.format("   ❌ %s execution failed !",ctx.provideType()));
        }
    }

    private ExecutionResult executeProvider(String providerType, MigrationTask task, ExecutionContext context) {
        return ProviderFactory.getProvider(providerType)
            .map(provider -> {
                logger.infof("   Executing %s provider...", providerType);
                return provider.execute(task, context);
            }).orElse(ExecutionResult.failure("Provider not found: " + providerType));
    }
}