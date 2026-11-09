package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.BuildFile;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * State shared by the UI checks of a validation run: the indexed target project, the optional source
 * project, and the report the results are added to.
 *
 * Rules added through a context created with {@link #scoped(String)} get the scope appended to their
 * name, e.g. {@code no_jsp_files (jsp-qute)}, to tell apart the rules of several migration paths.
 */
final class UiContext {

    /** Maximum number of offending files listed in the evidence of a failed rule. */
    private static final int SAMPLE_SIZE = 3;

    private final ProjectFiles target;
    private final ProjectFiles source;
    private final ValidationReport report;
    private final String scope;

    UiContext(ProjectFiles target, ProjectFiles source, ValidationReport report) {
        this(target, source, report, null);
    }

    private UiContext(ProjectFiles target, ProjectFiles source, ValidationReport report, String scope) {
        this.target = target;
        this.source = source;
        this.report = report;
        this.scope = scope;
    }

    UiContext scoped(String scope) {
        return new UiContext(target, source, report, scope);
    }

    ProjectFiles target() {
        return target;
    }

    /** The original Spring project. */
    ProjectFiles source() {
        return source;
    }

    BuildFile buildFile() {
        return target.buildFile();
    }

    // -- reporting -------------------------------------------------------

    ValidationRule rule(String name, String description) {
        return new ValidationRule(scope == null ? name : name + " (" + scope + ")", description);
    }

    void pass(ValidationRule rule, String evidence) {
        report.add(new ValidationResult(rule, Status.PASSED, evidence));
    }

    void fail(ValidationRule rule, String evidence) {
        report.add(new ValidationResult(rule, Status.FAILED, evidence));
    }

    void warn(ValidationRule rule, String evidence) {
        report.add(new ValidationResult(rule, Status.WARNING, evidence));
    }

    void result(ValidationRule rule, boolean passed, String passEvidence, String failEvidence) {
        if (passed) {
            pass(rule, passEvidence);
        } else {
            fail(rule, failEvidence);
        }
    }

    /**
     * Passes when there is no offender, otherwise fails with "{failLabel} in N files: a, b, c (and M more)".
     * Offenders are relative paths, optionally followed by details.
     */
    void offenders(ValidationRule rule, List<String> offenders, String passEvidence, String failLabel) {
        result(rule, offenders.isEmpty(), passEvidence,
                failLabel + " in " + offenders.size() + " files: " + sample(offenders));
    }

    /** Reports the files among {@code files} that match {@code isOffender}. */
    void offendingFiles(ValidationRule rule, Collection<Path> files, Predicate<Path> isOffender,
            String passEvidence, String failLabel) {
        offenders(rule, files.stream().filter(isOffender).map(target::relative).toList(), passEvidence, failLabel);
    }

    /** Reports the Java sources of the target (comments removed) for which {@code isOffender} is true. */
    void offendingJavaSources(ValidationRule rule, Predicate<String> isOffender, String passEvidence,
            String failLabel) {
        offendingFiles(rule, target.javaFiles(), f -> isOffender.test(target.javaSource(f)), passEvidence,
                failLabel);
    }

    /** Fails when one of the target Qute templates matches one of the patterns. */
    void templatesDoNotMatch(String name, String description, List<Pattern> patterns, String passEvidence,
            String failLabel) {
        ValidationRule rule = rule(name, description);
        if (!target.isDirectory(UiPaths.TEMPLATES_DIR)) {
            pass(rule, "No templates directory");
            return;
        }
        offendingFiles(rule, UiPaths.templates(target), f -> matchesAny(target.read(f), patterns), passEvidence,
                failLabel);
    }

    /** Fails when files with one of the suffixes remain anywhere in the target project. */
    void noFiles(String name, String description, String label, List<String> suffixes) {
        ValidationRule rule = rule(name, description);
        List<String> files = target.find("", suffixes).stream().map(target::relative).toList();
        result(rule, files.isEmpty(), "No " + label + " files found",
                "Found " + files.size() + " " + label + " files: " + sample(files));
    }

    static boolean matchesAny(String content, Collection<Pattern> patterns) {
        return patterns.stream().anyMatch(p -> p.matcher(content).find());
    }

    static String sample(List<String> items) {
        String listed = String.join(", ", items.subList(0, Math.min(SAMPLE_SIZE, items.size())));
        return items.size() > SAMPLE_SIZE ? listed + " (and " + (items.size() - SAMPLE_SIZE) + " more)" : listed;
    }
}
