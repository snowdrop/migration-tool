package dev.snowdrop.transform.provider;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.transform.provider.model.ExecutionContext;
import dev.snowdrop.transform.provider.model.ExecutionResult;

/**
 * Interface for migration providers that handle different types of transformations based on YAML rule instructions (AI,
 * OpenRewrite, Manual).
 */
public interface MigrationProvider {

    /**
     * Gets the provider type identifier (ai, openrewrite, manual)
     *
     * @return the provider type string
     */
    String getProviderType();

    /**
     * Executes the migration transformation for the given task
     *
     * @param task
     *            the migration task containing the rule and instructions
     * @param context
     *            the execution context containing project path, settings, etc.
     *
     * @return the execution result with success status and details
     */
    ExecutionResult execute(MigrationTask task, ExecutionContext context);
}