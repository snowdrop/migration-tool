package dev.snowdrop.transform.provider.model;

import dev.snowdrop.transform.provider.ai.Assistant;

import java.nio.file.Path;

/**
 * Context information for provider execution including project settings and configuration.
 */
public record ExecutionContext(Path projectPath, boolean verbose, boolean dryRun, String provideType,
        Assistant assistant, String openRewriteMavenPluginVersion, String compositeRecipeName) {
}