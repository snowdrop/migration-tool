package dev.snowdrop.mtool.model.analyze;

import java.nio.file.Path;

/**
 * @param appPath Path to the application to analyze
 * @param rulesPath Path to the rules directory or file
 * @param sourceTechnology Source technology (e.g. "springboot")
 * @param targetTechnology Target technology (e.g. "quarkus")
 * @param verbose Enable verbose output
 * @param output Output format (json, csv, html)
 * @param scanner Scanner to use (openrewrite, treesitter, etc.)
 * @param openRewriteMavenPluginVersion OpenRewrite Maven plugin version
 */
public record Config(String appPath, Path rulesPath, String sourceTechnology, String targetTechnology,
        boolean verbose, String output, String scanner,
        String openRewriteMavenPluginVersion) {
}
