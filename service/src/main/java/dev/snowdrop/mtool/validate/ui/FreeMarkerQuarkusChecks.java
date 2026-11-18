package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.Dependency;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static dev.snowdrop.mtool.validate.ui.UiContext.matchesAny;

/**
 * FreeMarker preserved with the Quarkiverse {@code quarkus-freemarker} extension.
 */
final class FreeMarkerQuarkusChecks implements UiChecks {

    private static final Dependency QUARKUS_FREEMARKER = Dependency.of("io.quarkiverse.freemarker", "quarkus-freemarker");

    private static final List<Pattern> SPRING_MACROS = List.of(
            Pattern.compile("spring\\.ftl"),
            Pattern.compile("<@spring\\."));
    private static final Pattern TEMPLATE_PATH = Pattern.compile("@(?:io\\.quarkiverse\\.freemarker\\.)?TemplatePath\\b");

    @Override
    public void run(UiContext context) {
        UiMigrationType type = UiMigrationType.FREEMARKER_QUARKUS;
        ProjectFiles target = context.target();
        List<Path> templates = target.find("", type.sourceViewExtensions());
        FreeMarkerTemplateSet templateSet = FreeMarkerTemplateSet.of(target.applicationConfig());

        checkDependency(context);
        checkTemplateSetConfig(context, templateSet);
        checkTemplatesLocation(context, templates, templateSet);
        checkSourceTargetParity(context, type, templateSet);

        context.offenders(context.rule("no_spring_freemarker_properties",
                "spring.freemarker.* properties must be migrated to quarkus.freemarker.*"),
                target.applicationConfig().filesDeclaring("spring.freemarker").stream().map(target::relative).toList(),
                "No spring.freemarker.* properties found", "spring.freemarker.* properties");
        context.offendingFiles(context.rule("no_spring_macros",
                "Spring FreeMarker macros (spring.ftl) must be removed from templates"),
                templates, f -> matchesAny(target.read(f), SPRING_MACROS),
                "No Spring macros found", "Spring macros");
        context.offendingJavaSources(context.rule("no_spring_freemarker_beans",
                "FreeMarkerConfigurer and FreeMarkerViewResolver beans and imports must be removed"),
                source -> FreeMarkerQuteChecks.SPRING_FREEMARKER_JAVA_MARKERS.stream().anyMatch(source::contains),
                "No Spring FreeMarker beans found", "Spring FreeMarker beans/imports");
        context.offendingJavaSources(context.rule("no_model_and_view",
                "ModelAndView has no Quarkus equivalent and must be removed"),
                source -> source.contains("ModelAndView"), "No ModelAndView usage found", "ModelAndView");
        checkConfigurationInjection(context, templates);
    }

    private void checkDependency(UiContext context) {
        ValidationRule rule = context.rule("quarkus_freemarker_dependency", QUARKUS_FREEMARKER + " must be present");
        if (!context.buildFile().exists()) {
            context.fail(rule, "Build file not found");
            return;
        }
        String passEvidence = "quarkus-freemarker dependency present";
        String failEvidence = QUARKUS_FREEMARKER + " dependency not found";
        context.result(rule, context.buildFile().hasDependency(QUARKUS_FREEMARKER),
                passEvidence, failEvidence);
    }

    /** Setting base-path without includes is a build-time error of the extension. */
    private void checkTemplateSetConfig(UiContext context, FreeMarkerTemplateSet templateSet) {
        ValidationRule rule = context.rule("freemarker_template_set_config",
                FreeMarkerTemplateSet.BASE_PATH + " requires " + FreeMarkerTemplateSet.INCLUDES);
        if (templateSet.isDefault()) {
            context.pass(rule, "Default template set (" + FreeMarkerTemplateSet.DEFAULT_BASE_PATH + ", **)");
            return;
        }
        String passEvidence = "Template set: base-path=" + templateSet.basePath().orElse("") + ", includes="
                + templateSet.includes().orElse("");
        String failEvidence = FreeMarkerTemplateSet.BASE_PATH + " is set without " + FreeMarkerTemplateSet.INCLUDES
                + ": the Quarkus build fails. Set " + FreeMarkerTemplateSet.INCLUDES
                + " (e.g. **), or remove both to use " + FreeMarkerTemplateSet.DEFAULT_BASE_PATH;
        context.result(rule, !templateSet.isMissingIncludes(), passEvidence, failEvidence);
    }

    private void checkTemplatesLocation(UiContext context, List<Path> templates, FreeMarkerTemplateSet templateSet) {
        ProjectFiles target = context.target();
        String templatesDir = templateSet.templatesDir();
        Path dir = target.resolve(templatesDir);
        List<String> misplaced = templates.stream()
                .filter(t -> !t.startsWith(dir))
                .map(target::relative)
                .toList();
        ValidationRule rule = context.rule("freemarker_templates_location",
                "FreeMarker templates must be located in the template set directory");
        String passEvidence = templates.size() + " FreeMarker templates in " + templatesDir;
        String failLabel = "FreeMarker templates outside " + templatesDir;
        context.offenders(rule, misplaced, passEvidence, failLabel);
    }

    /** FreeMarker is preserved, so every template must be kept. */
    private void checkSourceTargetParity(UiContext context, UiMigrationType type, FreeMarkerTemplateSet templateSet) {
        ValidationRule rule = context.rule("source_target_parity", "All FreeMarker templates must be preserved");
        int sourceCount = context.source().find("", type.sourceViewExtensions()).size();
        int targetCount = context.target().find(templateSet.templatesDir(), type.sourceViewExtensions()).size();
        String passEvidence = "FreeMarker parity: " + sourceCount + " source → " + targetCount + " target";
        String failEvidence = "Missing FreeMarker templates in target: " + sourceCount + " source → " + targetCount + " target";
        context.result(rule, targetCount >= sourceCount, passEvidence, failEvidence);
    }

    private void checkConfigurationInjection(UiContext context, List<Path> templates) {
        String ruleDescription = "Resources must render templates with an injected freemarker.template.Configuration or "
                + "an @TemplatePath Template";
        ValidationRule rule = context.rule("freemarker_configuration_injection", ruleDescription);
        if (templates.isEmpty()) {
            context.pass(rule, "No FreeMarker templates to render");
            return;
        }
        ProjectFiles target = context.target();
        boolean renders = target.javaFiles().stream().map(target::javaSource)
                .anyMatch(s -> (s.contains("freemarker.template.Configuration") || TEMPLATE_PATH.matcher(s).find())
                        && s.contains(".process("));
        String passEvidence = "Templates are rendered via the quarkus-freemarker beans";
        String failEvidence = "No class renders templates via template.process() with an injected "
                + "freemarker.template.Configuration or an @TemplatePath Template";
        context.result(rule, renders, passEvidence, failEvidence);
    }
}
