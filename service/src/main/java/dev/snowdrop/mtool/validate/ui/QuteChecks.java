package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ApplicationConfig;
import dev.snowdrop.mtool.project.JavaSources;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.BuildFile;
import dev.snowdrop.mtool.project.build.Dependency;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.snowdrop.mtool.validate.ui.UiContext.sample;
import static dev.snowdrop.mtool.validate.ui.UiPaths.TEMPLATES_DIR;

/**
 * Checks shared by every migration to Qute. They run once, even when several view technologies are
 * migrated, because those share the target templates directory.
 */
final class QuteChecks implements UiChecks {

    /** Fragments and includes may legitimately reduce the number of views after a Qute migration. */
    private static final double MIN_VIEW_RATIO = 0.5;

    private static final Dependency REST_QUTE = Dependency.of("io.quarkus", "quarkus-rest-qute");
    private static final Dependency QUTE = Dependency.of("io.quarkus", "quarkus-qute");

    private static final Pattern CHECKED_TEMPLATE_CLASS = Pattern.compile(
            "@CheckedTemplate\\s*(?:\\(([^)]*)\\))?\\s*(?:(?:public|protected|private|static|final|abstract)\\s+)*"
                    + "(?:class|interface)\\s+\\w+[^{]*\\{");
    private static final Pattern CHECKED_TEMPLATE_METHOD = Pattern
            .compile("\\bnative\\s+(?:[\\w.]+\\.)?(?:Mutable)?TemplateInstance\\s+([\\w$]+)\\s*\\(");
    private static final Pattern BASE_PATH = Pattern.compile("basePath\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern ENCLOSING_TYPE = Pattern.compile("\\b(class|interface|record|enum)\\s+\\w+[^;{]*\\{");
    private static final Pattern JAX_RS_PATH = Pattern.compile("@Path\\b");

    private final List<UiMigrationType> types;

    QuteChecks(List<UiMigrationType> types) {
        this.types = types;
    }

    @Override
    public void run(UiContext context) {
        checkQuteDependency(context);
        checkTemplateStructure(context);
        checkQuteConfiguration(context);
        checkCheckedTemplateLocations(context);
        checkMessageBundle(context);
        checkJaxRsEndpoints(context);
        checkSourceTargetParity(context);
    }

    // -- shared Qute checks ---------------------------------------------------

    private void checkQuteDependency(UiContext context) {
        ValidationRule rule = context.rule("quarkus_rest_qute_dependency", "quarkus-rest-qute dependency must be present");
        BuildFile buildFile = context.buildFile();
        if (!buildFile.exists()) {
            context.fail(rule, "Build file not found (neither pom.xml nor build.gradle)");
        } else if (buildFile.hasDependency(REST_QUTE)) {
            context.pass(rule, "quarkus-rest-qute dependency present");
        } else if (buildFile.hasDependency(QUTE)) {
            String evidence = "quarkus-qute dependency present without quarkus-rest-qute. This is only valid for "
                    + "non-REST templates (mail/PDF); use quarkus-rest-qute to serve templates via REST endpoints";
            context.warn(rule, evidence);
        } else {
            context.fail(rule, "Neither quarkus-qute nor quarkus-rest-qute dependency found");
        }
    }

    private void checkTemplateStructure(UiContext context) {
        ValidationRule rule = context.rule("qute_template_structure", "Qute templates must be in " + TEMPLATES_DIR);
        if (!context.target().isDirectory(TEMPLATES_DIR)) {
            context.fail(rule, "templates directory not found at " + TEMPLATES_DIR);
            return;
        }
        int templates = UiPaths.templates(context.target()).size();
        String passEvidence = "Found " + templates + " Qute templates";
        String failEvidence = "No .html template files found in templates directory";
        context.result(rule, templates > 0, passEvidence, failEvidence);
    }

    private void checkQuteConfiguration(UiContext context) {
        ValidationRule rule = context.rule("qute_configuration",
                "Qute configuration should be present in application.properties/yml");
        ApplicationConfig config = context.target().applicationConfig();
        if (!config.exists()) {
            String evidence = "No application.properties/yml found (configuration may be in other profiles)";
            context.pass(rule, evidence);
            return;
        }
        StringBuilder evidence = new StringBuilder("Qute configuration present");
        for (String property : List.of("quarkus.qute.strict-rendering", "quarkus.qute.property-not-found-strategy")) {
            if (config.get(property).isEmpty()) {
                evidence.append(". Consider adding ").append(property);
            }
        }
        context.pass(rule, evidence.toString());
    }

    /**
     * With @CheckedTemplate, templates are located after the enclosing class (nested declaration) or the
     * basePath, e.g. templates/TodoResource/todoDetail.html.
     */
    private void checkCheckedTemplateLocations(UiContext context) {
        ValidationRule rule = context.rule("checked_template_locations",
                "Templates of @CheckedTemplate methods must exist at the expected location");
        ProjectFiles target = context.target();
        Path templatesDir = target.resolve(TEMPLATES_DIR);
        boolean used = false;
        int checked = 0;
        List<String> missing = new ArrayList<>();

        for (Path javaFile : target.javaFiles()) {
            String content = target.javaSource(javaFile);
            Matcher declaration = CHECKED_TEMPLATE_CLASS.matcher(content);
            while (declaration.find()) {
                used = true;
                String args = declaration.group(1) == null ? "" : declaration.group(1);
                if (args.contains("defaultName")) {
                    continue; // custom naming strategy, cannot be derived statically
                }
                Path dir = templateDirectory(templatesDir, javaFile, content, declaration, args);
                String body = content.substring(declaration.end(),
                        JavaSources.closingBrace(content, declaration.end() - 1));
                Matcher method = CHECKED_TEMPLATE_METHOD.matcher(body);
                while (method.find()) {
                    String templateName = method.group(1);
                    if (templateName.contains("$")) {
                        continue; // fragment
                    }
                    checked++;
                    if (!target.hasFileNamed(dir, templateName)) {
                        missing.add(target.relative(dir) + "/" + templateName + ".html");
                    }
                }
            }
        }

        if (!used) {
            context.pass(rule, "@CheckedTemplate not used");
        } else {
            String passEvidence = "All " + checked + " checked templates found";
            String failEvidence = "Missing templates for @CheckedTemplate methods (" + missing.size() + "): " + sample(missing);
            context.result(rule, missing.isEmpty(), passEvidence, failEvidence);
        }
    }

    private static Path templateDirectory(Path templatesDir, Path javaFile, String content, Matcher declaration,
            String args) {
        Matcher basePath = BASE_PATH.matcher(args);
        if (basePath.find()) {
            return templatesDir.resolve(basePath.group(1).replaceFirst("^/+", ""));
        }
        if (ENCLOSING_TYPE.matcher(content.substring(0, declaration.start())).find()) {
            String fileName = javaFile.getFileName().toString();
            return templatesDir.resolve(fileName.substring(0, fileName.length() - ".java".length()));
        }
        return templatesDir;
    }

    private void checkMessageBundle(UiContext context) {
        ValidationRule rule = context.rule("qute_message_bundle",
                "Templates using {msg:...} require a @MessageBundle interface");
        ProjectFiles target = context.target();
        if (UiPaths.templates(target).stream().noneMatch(f -> target.read(f).contains("{msg:"))) {
            context.pass(rule, "No {msg:...} message keys used in templates");
            return;
        }
        boolean hasBundle = target.javaFiles().stream().anyMatch(f -> target.javaSource(f).contains("@MessageBundle"));
        String passEvidence = "{msg:...} keys used and a @MessageBundle interface is defined";
        String failEvidence = "Templates use {msg:...} but no @MessageBundle interface was found";
        context.result(rule, hasBundle, passEvidence, failEvidence);
    }

    private void checkJaxRsEndpoints(UiContext context) {
        ValidationRule rule = context.rule("jax_rs_endpoints", "Templates must be rendered through JAX-RS @Path resources");
        ProjectFiles target = context.target();
        if (UiPaths.templates(target).isEmpty()) {
            context.pass(rule, "No templates to render");
            return;
        }
        long resources = target.javaFiles().stream()
                .filter(f -> JAX_RS_PATH.matcher(target.javaSource(f)).find())
                .count();
        String passEvidence = "Found " + resources + " JAX-RS resource classes";
        String failEvidence = "No JAX-RS @Path resource found to render the templates";
        context.result(rule, resources > 0, passEvidence, failEvidence);
    }

    /** Compares the views of every migrated technology in the source with the Qute templates of the target. */
    private void checkSourceTargetParity(UiContext context) {
        ValidationRule rule = context.rule("source_target_parity",
                "Target should have a comparable number of views as the source");
        int sourceCount = types.stream().mapToInt(t -> countSourceViews(context.source(), t)).sum();
        int targetCount = UiPaths.templates(context.target()).size();

        if (sourceCount == 0) {
            String names = types.stream().map(UiMigrationType::getValue).collect(Collectors.joining(", "));
            String evidence = "No " + names + " source views found in the source project. "
                    + "Verify the UI migration type and the source project path";
            context.warn(rule, evidence);
        } else {
            String passEvidence = "View parity check: " + sourceCount + " source → " + targetCount + " target";
            String failEvidence = "Significant view count difference: " + sourceCount + " source views → " + targetCount
                    + " target templates. Verify no migration loss.";
            context.result(rule, targetCount >= sourceCount * MIN_VIEW_RATIO, passEvidence, failEvidence);
        }
    }

    /**
     * Thymeleaf views are only counted in the templates directory, because the other .html files of the
     * source (static pages, test resources) are not views.
     */
    private static int countSourceViews(ProjectFiles source, UiMigrationType type) {
        String dir = type == UiMigrationType.THYMELEAF_QUTE ? TEMPLATES_DIR : "src/main";
        return source.find(dir, type.sourceViewExtensions()).size();
    }
}
