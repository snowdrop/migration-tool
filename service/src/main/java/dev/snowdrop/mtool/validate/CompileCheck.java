package dev.snowdrop.mtool.validate;

import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.build.BuildFile.BuildSystem;
import dev.snowdrop.mtool.project.build.BuildRunner;

import java.nio.file.Path;
import java.util.List;

/**
 * Compiles a project and reports the outcome, for the validators that check the project compiles.
 * Runs {@code mvn compile} for Maven projects and {@code gradle compileJava} for Gradle projects
 * (using the project's wrapper when present). Maven wins when the project has both build files.
 */
public final class CompileCheck {

    private CompileCheck() {
    }

    /**
     * Compiles the project, reporting the result under a rule named after the command that ran
     * ({@code mvn compile} or {@code gradle compileJava}).
     */
    public static ValidationResult run(String description, Path projectPath) {
        BuildSystem buildSystem = BuildSystem.detect(projectPath);
        String name = buildSystem == BuildSystem.GRADLE ? "gradle compileJava" : "mvn compile";
        return run(new ValidationRule(name, description), projectPath);
    }

    public static ValidationResult run(ValidationRule rule, Path projectPath) {
        BuildSystem buildSystem = BuildSystem.detect(projectPath);
        if (buildSystem == null) {
            return new ValidationResult(rule, Status.FAILED,
                    "Cannot compile — no build file found (pom.xml, build.gradle or build.gradle.kts)");
        }
        List<String> command = buildSystem == BuildSystem.MAVEN
                ? BuildRunner.mavenCommand(projectPath, "compile")
                : BuildRunner.gradleCommand(projectPath, "compileJava");
        String label = buildSystem == BuildSystem.MAVEN ? "mvn compile" : "gradle compileJava";

        BuildRunner.Result build = BuildRunner.run(projectPath, command);
        return switch (build.outcome()) {
            case SUCCESS -> new ValidationResult(rule, Status.PASSED, label + " — BUILD SUCCESS");
            case FAILURE -> new ValidationResult(rule, Status.FAILED, label + " — BUILD FAILURE"
                    + (build.details().isEmpty() ? "" : ": " + build.details()));
            case TIMEOUT, INTERRUPTED -> new ValidationResult(rule, Status.FAILED, label + " " + build.details());
            // The project was not verified, so a build that cannot start is a failure, not a warning
            case NOT_RUNNABLE -> new ValidationResult(rule, Status.FAILED,
                    "Could not run " + label + ": " + build.details());
        };
    }
}
