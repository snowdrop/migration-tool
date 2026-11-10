package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JSF to Qute: no XHTML view, JSF component, JSF annotation or JSF configuration remains.
 */
final class JsfQuteChecks implements UiChecks {

    private static final List<Pattern> COMPONENTS = Patterns.caseInsensitive(
            "<h:", "<f:", "<ui:", "<p:", "xmlns:h=", "xmlns:f=", "xmlns:ui=", "xmlns:p=");
    private static final List<String> ANNOTATION_MARKERS = List.of(
            "@ManagedBean", "@ManagedProperty", "@FacesConverter", "@FacesValidator", "@ViewScoped",
            "javax.faces", "jakarta.faces");

    @Override
    public void run(UiContext context) {
        context.noFiles("no_jsf_xhtml_files", "No JSF XHTML files remain", "XHTML",
                UiMigrationType.JSF_QUTE.sourceViewExtensions());
        context.templatesDoNotMatch("no_jsf_components", "No JSF components remain in templates",
                COMPONENTS, "No JSF components found", "JSF components");
        context.offendingJavaSources(context.rule("no_jsf_managed_beans",
                "No JSF annotations (@ManagedBean, @FacesConverter, @FacesValidator, @ViewScoped) or imports remain"),
                source -> ANNOTATION_MARKERS.stream().anyMatch(source::contains),
                "No JSF managed beans or annotations found", "JSF managed beans/annotations");
        checkNoFacesConfig(context);
    }

    private void checkNoFacesConfig(UiContext context) {
        ProjectFiles target = context.target();
        List<String> leftovers = new ArrayList<>();
        target.find("", "faces-config.xml").forEach(f -> leftovers.add(target.relative(f)));
        for (var webXml : target.find("", "web.xml")) {
            String content = target.read(webXml);
            if (content.contains("FacesServlet") || content.contains("javax.faces.webapp")) {
                leftovers.add(target.relative(webXml) + " (JSF servlet mapping)");
            }
        }
        String description = "faces-config.xml and the JSF servlet mapping in web.xml must be removed";
        String passEvidence = "No faces-config.xml or JSF servlet mapping found";
        String failLabel = "JSF configuration";
        ValidationRule rule = context.rule("no_faces_config", description);
        context.offenders(rule, leftovers, passEvidence, failLabel);
    }

}
