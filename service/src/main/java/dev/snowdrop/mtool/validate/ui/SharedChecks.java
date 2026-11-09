package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static dev.snowdrop.mtool.validate.ui.UiContext.matchesAny;
import static dev.snowdrop.mtool.validate.ui.UiPaths.META_INF_RESOURCES;
import static dev.snowdrop.mtool.validate.ui.UiPaths.TEMPLATES_DIR;
import static dev.snowdrop.mtool.validate.ui.UiPaths.WEBAPP_DIR;

/**
 * Checks shared by every view technology: static resources location and removal of the Spring Security
 * CSRF tokens.
 */
final class SharedChecks implements UiChecks {

    private static final List<String> STATIC_RESOURCE_EXTENSIONS = List.of(
            ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico");
    private static final List<String> SPRING_STATIC_LOCATIONS = List.of(
            "src/main/resources/static", WEBAPP_DIR);

    private static final List<Pattern> CSRF_PATTERNS = List.of(
            Pattern.compile("_csrf\\.(token|parameterName|headerName)"),
            Pattern.compile("_csrf_header"),
            Pattern.compile("name=\"_csrf\""));

    @Override
    public void run(UiContext context) {
        checkStaticResourcesLocation(context);
        checkNoCsrfTokens(context);
    }

    private void checkStaticResourcesLocation(UiContext context) {
        ProjectFiles target = context.target();
        List<String> misplaced = SPRING_STATIC_LOCATIONS.stream()
                .flatMap(dir -> target.find(dir, STATIC_RESOURCE_EXTENSIONS).stream())
                .map(target::relative)
                .toList();
        ValidationRule rule = context.rule("static_resources_location", "Static resources must be in META-INF/resources/");
        String passEvidence = "Static resources in correct location";
        String failLabel = "Resources in old locations (should be in META-INF/resources/)";
        context.offenders(rule, misplaced, passEvidence, failLabel);
    }

    private void checkNoCsrfTokens(UiContext context) {
        ProjectFiles target = context.target();
        List<Path> files = new ArrayList<>(target.find(TEMPLATES_DIR, ".html"));
        files.addAll(target.find(ProjectFiles.RESOURCES_DIR, ".ftl", ".ftlh", ".ftlx"));
        files.addAll(target.find(META_INF_RESOURCES, ".js", ".xhtml", ".html"));
        ValidationRule rule = context.rule("no_csrf_tokens", "Spring Security CSRF tokens must be removed");
        context.offendingFiles(rule,
                files, f -> hasCsrfToken(f, target.read(f)), "No CSRF tokens found", "CSRF tokens");
    }

    /** JavaScript reads the token from the meta tags, so any _csrf reference is a leftover. */
    private static boolean hasCsrfToken(Path file, String content) {
        return file.getFileName().toString().endsWith(".js")
                ? content.toLowerCase(Locale.ROOT).contains("_csrf")
                : matchesAny(content, CSRF_PATTERNS);
    }
}
