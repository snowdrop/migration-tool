package dev.snowdrop.mtool.commands;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.validate.ProjectSetupValidator;
import dev.snowdrop.mtool.validate.Validator;
import dev.snowdrop.mtool.validate.persistence.PersistenceValidator;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;

@CommandLine.Command(name = "validate", description = "Validate that a migration is correct and complete")
public class ValidateCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(ValidateCommand.class);

    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    @CommandLine.Parameters(index = "0", description = "Validator to run (e.g. project-setup, persistence)")
    public String validatorName;

    @CommandLine.Parameters(index = "1", description = "Path to the project to validate")
    public String appPath;

    @CommandLine.Option(names = { "-v",
            "--verbose" }, description = "Enable verbose output")
    public boolean verbose;

    @CommandLine.Option(names = { "--skip-compile" }, description = "Skip the mvn compile check")
    public boolean skipCompile;

    @CommandLine.Option(names = {
            "--spring-project" }, description = "Path to the original Spring project (required for persistence validator)")
    public String springProjectPath;

    @Override
    public void run() {
        Path projectPath = resolvePath(appPath);
        if (!projectPath.toFile().exists()) {
            logger.errorf("Project path does not exist: %s", appPath);
            return;
        }

        Validator validator = resolveValidator();
        if (validator == null) {
            logger.errorf("Unknown validator: %s", validatorName);
            return;
        }

        logger.infof("Validating project: %s", projectPath);
        ValidationReport report = validator.validate(projectPath);
        printReport(report);
    }

    private Validator resolveValidator() {
        List<Validator> validators = new ArrayList<>();
        validators.add(new ProjectSetupValidator(skipCompile));

        if (springProjectPath != null) {
            Path springPath = resolvePath(springProjectPath);
            validators.add(new PersistenceValidator(springPath, skipCompile));
        } else if ("persistence".equals(validatorName)) {
            logger.error("--spring-project is required for the persistence validator");
            return null;
        }

        return validators.stream()
                .filter(v -> v.name().equals(validatorName))
                .findFirst()
                .orElse(null);
    }

    private void printReport(ValidationReport report) {
        String separator = "=".repeat(70);

        System.out.println();
        System.out.println(separator);
        System.out.printf("%sVerification Summary — %s (Spring to Quarkus)%s%n",
                BOLD, report.getValidatorName(), RESET);
        System.out.println(separator);

        String statusColor = switch (report.overallStatus()) {
            case "PASS" -> GREEN;
            case "FAIL" -> RED;
            default -> YELLOW;
        };
        System.out.printf("Status  : %s%s%s%n", statusColor, report.overallStatus(), RESET);
        System.out.printf("Rules   : %d total  |  %d passed  |  %d failed  |  %d warnings%n",
                report.totalRules(), report.passed(), report.failed(), report.warnings());
        System.out.println();

        for (ValidationResult result : report.getResults()) {
            String icon;
            String color;
            switch (result.status()) {
                case PASSED -> {
                    icon = "✓";
                    color = GREEN;
                }
                case FAILED -> {
                    icon = "✗";
                    color = RED;
                }
                default -> {
                    icon = "!";
                    color = YELLOW;
                }
            }
            System.out.printf("  %s%s%s %s%n", color, icon, RESET, result.rule().name());
            System.out.printf("      %s%n", result.evidence());
        }

        System.out.println(separator);
        System.out.println();
    }
}