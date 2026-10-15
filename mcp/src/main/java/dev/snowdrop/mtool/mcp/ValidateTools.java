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
import dev.snowdrop.mtool.validate.ProjectSetupValidator;
import dev.snowdrop.mtool.validate.Validator;
import dev.snowdrop.mtool.validate.persistence.PersistenceValidator;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.jboss.logging.Logger;

public class ValidateTools {

    private static final Logger LOG = Logger.getLogger(ValidateTools.class);

    @Tool(description = "Validate that a Spring-to-Quarkus migration is correct and complete. "
            + "Runs validators (project-setup, persistence) against the migrated Quarkus project, "
            + "comparing it with the original Spring project to detect missing entities, "
            + "field mismatches, ID strategy changes, and relationship differences.")
    public String sourceValidate(
            @ToolArg(description = "Path to the migrated Quarkus project to validate") String target,
            @ToolArg(description = "Path to the original Spring project (required for persistence validation)") String source,
            @ToolArg(description = "Validator to run: 'project-setup', 'persistence', or 'all'", defaultValue = "all") String validator,
            @ToolArg(description = "Skip the mvn compile check", defaultValue = "false") String skipCompile,
            @ToolArg(description = "Java parser to use: 'treesitter' (default) or 'javaparser'", defaultValue = "treesitter") String parser) {

        Path targetPath = Paths.get(target).toAbsolutePath();
        Path sourcePath = Paths.get(source).toAbsolutePath();
        boolean skip = Boolean.parseBoolean(skipCompile);

        if (!targetPath.toFile().exists()) {
            return "Error: target project path does not exist: " + target;
        }
        if (!sourcePath.toFile().exists()) {
            return "Error: source project path does not exist: " + source;
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

        if (validators.isEmpty()) {
            return "Error: unknown validator '" + validator + "'. Use 'project-setup', 'persistence', or 'all'.";
        }

        StringBuilder sb = new StringBuilder();
        for (Validator v : validators) {
            LOG.infof("Running validator '%s' on target: %s", v.name(), targetPath);
            ValidationReport report = v.validate(targetPath);
            sb.append(formatReport(report));
            sb.append("\n");
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
