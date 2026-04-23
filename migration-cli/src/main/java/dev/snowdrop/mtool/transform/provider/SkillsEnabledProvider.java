package dev.snowdrop.mtool.transform.provider;

import dev.snowdrop.mtool.transform.provider.model.ExecutionContext;
import dev.snowdrop.mtool.transform.provider.model.ExecutionResult;

public interface SkillsEnabledProvider extends MigrationProvider {
    ExecutionResult execute(String skillPath, ExecutionContext context);
}