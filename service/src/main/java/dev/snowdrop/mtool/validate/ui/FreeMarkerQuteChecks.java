package dev.snowdrop.mtool.validate.ui;

import java.util.List;
import java.util.regex.Pattern;

/**
 * FreeMarker to Qute: no FreeMarker template, directive, import or Spring FreeMarker configuration remains.
 */
final class FreeMarkerQuteChecks implements UiChecks {

    static final List<String> SPRING_FREEMARKER_JAVA_MARKERS = List.of(
            "FreeMarkerConfigurer", "FreeMarkerViewResolver",
            "org.springframework.web.servlet.view.freemarker", "org.springframework.ui.freemarker");

    private static final List<Pattern> DIRECTIVES = List.of(
            Pattern.compile("</?#[a-zA-Z]"),
            Pattern.compile("<@[a-zA-Z]"));

    @Override
    public void run(UiContext context) {
        context.noFiles("no_freemarker_files", "No FreeMarker files remain", "FreeMarker",
                UiMigrationType.FREEMARKER_QUTE.sourceViewExtensions());
        context.templatesDoNotMatch("no_freemarker_directives",
                "No FreeMarker directives or macro calls remain in templates",
                DIRECTIVES, "No FreeMarker directives found", "FreeMarker directives");
        context.offendingJavaSources(context.rule("no_freemarker_java",
                "No FreeMarker imports or Spring FreeMarker configuration classes remain"),
                FreeMarkerQuteChecks::usesFreeMarker,
                "No FreeMarker imports or configuration classes found", "FreeMarker usage");
    }

    private static boolean usesFreeMarker(String source) {
        return source.contains("import freemarker.") || source.contains("import static freemarker.")
                || SPRING_FREEMARKER_JAVA_MARKERS.stream().anyMatch(source::contains);
    }
}
