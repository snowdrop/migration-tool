package dev.snowdrop.mtool.transform.provider.model;

import java.nio.file.Path;

/**
 * Context information for provider execution including project settings and configuration.
 */
public record ExecutionContext(Path projectPath, boolean verbose, boolean dryRun, String provideType,
        String openRewriteMavenPluginVersion, String compositeRecipeName) {
}
