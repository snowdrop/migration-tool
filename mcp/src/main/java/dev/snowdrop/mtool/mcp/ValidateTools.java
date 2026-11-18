package dev.snowdrop.mtool.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.scanner.analyzer.JavaAnalyzer;
import dev.snowdrop.mtool.scanner.analyzer.JavaParserAnalyzer;
import dev.snowdrop.mtool.scanner.analyzer.TreeSitterAnalyzer;
import dev.snowdrop.mtool.validate.project.ProjectSetupValidator;
import dev.snowdrop.mtool.validate.persistence.PersistenceValidator;
import dev.snowdrop.mtool.validate.ui.UiMigrationType;
import dev.snowdrop.mtool.validate.ui.UiValidator;
import dev.snowdrop.mtool.validate.Validator;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolGuardrails;
import org.jboss.logging.Logger;

public class ValidateTools {

    private static final Logger LOG = Logger.getLogger(ValidateTools.class);

    @ToolGuardrails(input = { FilesystemAccessGuardrail.class, ValidatorNameGuardrail.class })
    @Tool(name = "quarkit_validate", description = "Validate that a Spring to Quarkus migration is correct and complete. "
            + "Runs validators (project-setup, persistence, ui, messaging, REST, transaction, etc) against the migrated Quarkus project, "
            + "comparing it with the original Spring project: missing entities, field mismatches, ID strategy changes, "
            + "relationship differences, and view layer (templates, static resources, legacy view technology) issues.")
    public String validate(
            @ToolArg(description = "Path to the migrated Quarkus project to validate") String target,
            @ToolArg(description = "Path to the original Spring project (required for the persistence, ui and all "
                    + "validators)", required = false) String source,
            @ToolArg(description = "Validator to run: 'project-setup', 'persistence', 'ui', or 'all'", defaultValue = "all") String validator,
            @ToolArg(description = "Skip the build check", defaultValue = "false") String skipCompile,
            @ToolArg(description = "Java parser to use: 'treesitter' (default) or 'javaparser'", defaultValue = "treesitter") String parser,
            @ToolArg(description = "UI migration type(s), required by the 'ui' validator: 'jsp-qute', 'thymeleaf-qute', 'freemarker-qute', "
                    + "'freemarker-quarkus', 'jsf-qute' or 'jsf-myfaces'. Use a comma-separated list when the project mixes view technologies "
                    + "(e.g. 'thymeleaf-qute,jsp-qute'). When validator is 'all' the ui validator only runs if this is set", defaultValue = "") String uiMigrationType) {

        Path targetPath = Paths.get(target).toAbsolutePath();
        Path sourcePath = source == null || source.isBlank() ? null : Paths.get(source).toAbsolutePath();
        boolean skip = Boolean.parseBoolean(skipCompile);

        if (!targetPath.toFile().exists()) {
            return "Error: target project path does not exist: " + target;
        }
        if (sourcePath != null && !sourcePath.toFile().exists()) {
            return "Error: source project path does not exist: " + source;
        }
        boolean needsSource = "all".equals(validator) || "persistence".equals(validator) || "ui".equals(validator);
        if (sourcePath == null && needsSource) {
            return "Error: source project path is required for the '" + validator + "' validator";
        }

        JavaAnalyzer analyzer = "javaparser".equalsIgnoreCase(parser)
                ? new JavaParserAnalyzer()
                : new TreeSitterAnalyzer();

        List<Validator> validators = new ArrayList<>();
        if ("all".equals(validator) || "project-setup".equals(validator)) {
            validators.add(new ProjectSetupValidator(skip));
        }
        if ("all".equals(validator) || "persistence".equals(validator)) {
            validators.add(new PersistenceValidator(analyzer, sourcePath, skip));
        }
        boolean hasUiType = uiMigrationType != null && !uiMigrationType.isBlank();
        String skippedUi = null;
        if ("all".equals(validator) && !hasUiType) {
            skippedUi = "## ui\nStatus: SKIPPED\n\n[WARN] ui — not run: set uiMigrationType to validate the view layer\n";
        }
        if ("ui".equals(validator) || ("all".equals(validator) && hasUiType)) {
            try {
                validators.add(new UiValidator(UiMigrationType.fromValues(uiMigrationType), sourcePath, skip));
            } catch (IllegalArgumentException e) {
                return "Error: " + e.getMessage();
            }
        }

        if (validators.isEmpty()) {
            return "Error: unknown validator '" + validator + "'. Use 'project-setup', 'persistence', 'ui', or 'all'.";
        }

        StringBuilder sb = new StringBuilder();
        for (Validator v : validators) {
            LOG.infof("Running validator '%s' on target: %s", v.name(), targetPath);
            ValidationReport report = v.validate(targetPath);
            sb.append(formatReport(report));
            sb.append("\n");
        }
        if (skippedUi != null) {
            sb.append(skippedUi);
        }

        return sb.toString().trim();
    }

    private String formatReport(ValidationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(report.getValidatorName()).append("\n");
        sb.append("Status: ").append(report.overallStatus()).append("\n");
        sb.append(String.format("Rules: %d total | %d passed | %d failed | %d warnings%n",
                report.totalRules(), report.passed(), report.failed(), report.warnings()));
        sb.append("\n");

        for (ValidationResult result : report.getResults()) {
            String icon = switch (result.status()) {
                case PASSED -> "[PASS]";
                case FAILED -> "[FAIL]";
                default -> "[WARN]";
            };
            sb.append(String.format("%s %s — %s%n", icon, result.rule().name(), result.evidence()));
        }

        return sb.toString();
    }
}
