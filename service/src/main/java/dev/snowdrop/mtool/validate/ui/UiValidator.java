package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.BuildFile;
import dev.snowdrop.mtool.project.build.BuildRunner;
import dev.snowdrop.mtool.validate.Validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the UI layer of a Spring to Quarkus migration.
 *
 * One or several migration paths can be validated in a single run, because a project may mix
 * several view technologies (see {@link UiMigrationType}). When more than one path is validated,
 * the rules specific to a path are suffixed with the path value, e.g. {@code no_jsp_files (jsp-qute)}.
 */
public class UiValidator implements Validator {

    private final Set<UiMigrationType> migrationTypes;
    private final Path sourceDir;
    private final boolean skipCompile;

    /**
     * @param migrationType UI migration path to validate
     * @param sourceDir path of the original Spring project, compared with the migrated project
     * @param skipCompile skip the build verification
     */
    public UiValidator(UiMigrationType migrationType, Path sourceDir, boolean skipCompile) {
        this(EnumSet.of(migrationType), sourceDir, skipCompile);
    }

    /**
     * @param migrationTypes UI migration paths to validate, for projects mixing several view technologies
     * @param sourceDir path of the original Spring project, compared with the migrated project
     * @param skipCompile skip the build verification
     * @throws IllegalArgumentException when no migration type is given, the types conflict, or the source
     *         project is missing
     */
    public UiValidator(Set<UiMigrationType> migrationTypes, Path sourceDir, boolean skipCompile) {
        if (migrationTypes == null || migrationTypes.isEmpty()) {
            String message = "At least one UI migration type is required. Valid types: " + UiMigrationType.validTypes();
            throw new IllegalArgumentException(message);
        }
        rejectConflict(migrationTypes, UiMigrationType.FREEMARKER_QUTE, UiMigrationType.FREEMARKER_QUARKUS);
        rejectConflict(migrationTypes, UiMigrationType.JSF_QUTE, UiMigrationType.JSF_MYFACES);
        this.migrationTypes = EnumSet.copyOf(migrationTypes);
        if (sourceDir == null || !Files.isDirectory(sourceDir)) {
            String message = "Source project not found: " + sourceDir
                    + ". The ui validator compares the migrated project with the original Spring project";
            throw new IllegalArgumentException(message);
        }
        this.sourceDir = sourceDir.toAbsolutePath();
        this.skipCompile = skipCompile;
    }

    /** The same view technology cannot be both migrated to Qute and preserved. */
    private static void rejectConflict(Set<UiMigrationType> types, UiMigrationType a, UiMigrationType b) {
        if (types.contains(a) && types.contains(b)) {
            String message = "UI migration types '" + a.getValue() + "' and '" + b.getValue()
                    + "' cannot be combined: the view technology is either migrated to Qute or preserved";
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public String name() {
        return "ui";
    }

    @Override
    public ValidationReport validate(Path projectPath) {
        ValidationReport report = new ValidationReport(name());
        ProjectFiles target = ProjectFiles.scan(projectPath);
        ProjectFiles source = ProjectFiles.scan(sourceDir);
        UiContext context = new UiContext(target, source, report);

        new SharedChecks().run(context);

        List<UiMigrationType> quteTypes = migrationTypes.stream().filter(UiMigrationType::isQute).toList();
        if (!quteTypes.isEmpty()) {
            new QuteChecks(quteTypes).run(context);
        }

        boolean multiple = migrationTypes.size() > 1;
        for (UiMigrationType type : migrationTypes) {
            UiContext typeContext = multiple ? context.scoped(type.getValue()) : context;
            checksFor(type).run(typeContext);
            new MigrationTypeChecks(type).run(typeContext);
        }

        checkQuarkusBuild(context);
        return report;
    }

    private static UiChecks checksFor(UiMigrationType type) {
        return switch (type) {
            case JSP_QUTE -> new JspChecks();
            case THYMELEAF_QUTE -> new ThymeleafChecks();
            case FREEMARKER_QUTE -> new FreeMarkerQuteChecks();
            case JSF_QUTE -> new JsfQuteChecks();
            case FREEMARKER_QUARKUS -> new FreeMarkerQuarkusChecks();
            case JSF_MYFACES -> new MyFacesChecks();
        };
    }

    /**
     * Packages the application: the Quarkus build step validates the Qute templates (and initializes the
     * FreeMarker and MyFaces extensions), which a plain compilation does not.
     */
    private void checkQuarkusBuild(UiContext context) {
        ValidationRule rule = context.rule("quarkus_build",
                "Quarkus build must succeed (validates the templates at build time)");
        if (skipCompile) {
            context.warn(rule, "Build skipped, templates were not validated at build time");
            return;
        }
        BuildFile buildFile = context.buildFile();
        if (!buildFile.exists()) {
            context.fail(rule, "Build file not found");
            return;
        }
        Path root = context.target().root();
        List<String> command = buildFile.buildSystem() == BuildFile.BuildSystem.MAVEN
                ? BuildRunner.mavenCommand(root, "clean", "package", "-DskipTests")
                : BuildRunner.gradleCommand(root, "clean", "build", "-x", "test");
        BuildRunner.Result build = BuildRunner.run(root, command);
        String commandLine = String.join(" ", command);
        switch (build.outcome()) {
            case SUCCESS -> context.pass(rule, commandLine + " succeeded");
            case FAILURE -> context.fail(rule, commandLine + " failed: " + build.details());
            case TIMEOUT, INTERRUPTED -> context.fail(rule, commandLine + " " + build.details());
            // The templates were not validated, so a build that cannot start is a failure, not a warning
            case NOT_RUNNABLE -> context.fail(rule, "Could not run " + commandLine + ": " + build.details());
        }
    }
}
