package dev.snowdrop.mtool.transform.provider.model;

import dev.snowdrop.mtool.transform.provider.ai.Assistant;
import dev.snowdrop.mtool.transform.provider.ai.SkillsAssistant;

import java.nio.file.Path;

/**
 * Context information for provider execution including project settings and configuration.
 */
public record ExecutionContext(Path projectPath, boolean verbose, boolean dryRun, String provideType,
        Assistant assistant, SkillsAssistant aiSkillsAssistant, String aiSkillsHomeDir, String openRewriteMavenPluginVersion,
        String compositeRecipeName) {
}