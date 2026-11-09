package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.BuildFile;
import dev.snowdrop.mtool.project.build.Dependency;

import java.util.List;
import java.util.regex.Pattern;

import static dev.snowdrop.mtool.validate.ui.UiContext.matchesAny;

/**
 * Checks driven by the data of a {@link UiMigrationType}, run for every migration path: the legacy
 * dependencies of the source view technology are removed and, for the Qute paths, its expression
 * language no longer appears in the templates.
 */
final class MigrationTypeChecks implements UiChecks {

    /** Inline scripts may legitimately contain ${...} (JavaScript template literals) or #{...}. */
    private static final Pattern SCRIPT_BLOCK = Pattern.compile("(?is)<script\\b[^>]*>.*?</script>");

    private final UiMigrationType type;

    MigrationTypeChecks(UiMigrationType type) {
        this.type = type;
    }

    @Override
    public void run(UiContext context) {
        if (!type.legacyDependencies().isEmpty()) {
            checkLegacyDependenciesRemoved(context);
        }
        if (!type.legacyExpressions().isEmpty()) {
            checkExpressionLanguageMigrated(context);
        }
    }

    private void checkLegacyDependenciesRemoved(UiContext context) {
        ValidationRule rule = context.rule("legacy_dependencies_removed",
                "Legacy view technology dependencies must be removed");
        BuildFile buildFile = context.buildFile();
        if (!buildFile.exists()) {
            context.pass(rule, "No build file to check");
            return;
        }
        List<String> remaining = type.legacyDependencies().stream()
                .filter(buildFile::hasDependency)
                .map(Dependency::toString)
                .toList();
        String passEvidence = "All legacy dependencies removed";
        String failEvidence = "Found " + remaining.size() + " legacy dependencies: " + String.join(", ", remaining);
        context.result(rule, remaining.isEmpty(), passEvidence, failEvidence);
    }

    private void checkExpressionLanguageMigrated(UiContext context) {
        ProjectFiles target = context.target();
        String description = "Legacy expression language must be migrated to Qute syntax";
        ValidationRule rule = context.rule("expression_language_migration", description);
        context.offendingFiles(rule,
                UiPaths.templates(target),
                f -> matchesAny(SCRIPT_BLOCK.matcher(target.read(f)).replaceAll(""), type.legacyExpressions()),
                "Expression language migrated", "Legacy EL");
    }
}
