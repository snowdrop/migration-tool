package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.project.ProjectFiles;
import dev.snowdrop.mtool.project.build.Dependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.snowdrop.mtool.validate.ui.UiContext.matchesAny;
import static dev.snowdrop.mtool.validate.ui.UiPaths.META_INF_RESOURCES;
import static dev.snowdrop.mtool.validate.ui.UiPaths.WEBAPP_DIR;

/**
 * JSF preserved with the Quarkus MyFaces extension.
 */
final class MyFacesChecks implements UiChecks {

    private static final Dependency MYFACES_QUARKUS = Dependency.of("org.apache.myfaces.core.extensions.quarkus",
            "myfaces-quarkus");
    private static final Dependency QUARKUS_PRIMEFACES = Dependency.of("io.quarkiverse.primefaces", "quarkus-primefaces");
    private static final String WEB_XML = "src/main/resources/META-INF/web.xml";

    private static final List<String> OLD_FACES_NAMESPACES = List.of("xmlns.jcp.org/jsf", "java.sun.com/jsf");
    private static final List<Pattern> JAVAX_IMPORTS = List.of(
            Pattern.compile("import\\s+javax\\.faces\\."),
            Pattern.compile("import\\s+javax\\.servlet\\.jsp\\."));
    private static final Pattern SPRING_SCOPE = Pattern.compile(
            "@(SessionScope|RequestScope|ApplicationScope)\\b"
                    + "|org\\.springframework\\.web\\.context\\.annotation\\.(Session|Request|Application)Scope");
    private static final Pattern FACES_CONTEXT_INJECTION = Pattern.compile("@Inject[^;{}]*?\\bFacesContext\\s+\\w+\\s*;");
    private static final Pattern SERIALIZABLE = Pattern.compile("implements\\s+[^{]*\\bSerializable\\b", Pattern.DOTALL);
    private static final Pattern MANAGED_BEAN = Pattern.compile("@(?:[\\w.]+\\.)?ManagedBean\\b");
    private static final Pattern MANAGED_PROPERTY = Pattern.compile("@(?:[\\w.]+\\.)?ManagedProperty\\b");
    private static final Pattern FACES_CONFIG_VERSION = Pattern.compile("<faces-config\\b[^>]*\\bversion\\s*=\\s*\"(\\d+)");
    private static final Pattern XML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern MANAGED_CONVERTER = managedPattern("@FacesConverter");
    private static final Pattern MANAGED_VALIDATOR = managedPattern("@FacesValidator");

    @Override
    public void run(UiContext context) {
        checkFacesExtension(context);
        checkPrimeFacesExtension(context);
        checkFacesServlet(context);
        checkJakartaNamespaces(context);
        checkFacesConfig(context);
        checkCdiBeans(context);
        context.offendingJavaSources(context.rule("no_spring_scope_annotations",
                "Spring scope annotations must be replaced by CDI scopes"),
                source -> SPRING_SCOPE.matcher(source).find(),
                "No Spring scope annotations found", "Spring scope annotations");
        context.offendingJavaSources(context.rule("no_faces_context_injection",
                "FacesContext must be obtained with FacesContext.getCurrentInstance(), not injected"),
                source -> FACES_CONTEXT_INJECTION.matcher(source).find(),
                "No FacesContext injection found", "FacesContext injected");
        checkManagedConvertersAndValidators(context);
        context.offendingJavaSources(context.rule("viewscoped_serializable", "@ViewScoped beans must implement Serializable"),
                source -> source.contains("@ViewScoped") && !SERIALIZABLE.matcher(source).find(),
                "All @ViewScoped beans implement Serializable", "@ViewScoped beans not Serializable");
        checkXhtmlLocation(context);
        context.offendingJavaSources(context.rule("javax_jakarta_imports", "javax.faces imports must be migrated to jakarta"),
                source -> matchesAny(source, JAVAX_IMPORTS), "All imports use jakarta.*", "javax.* imports");
        checkSourceTargetParity(context);
    }

    private void checkFacesExtension(UiContext context) {
        ValidationRule rule = context.rule("myfaces_dependency", "A Faces Quarkus extension must be present");
        if (!context.buildFile().exists()) {
            context.fail(rule, "Build file not found");
            return;
        }
        String passEvidence = "Faces Quarkus extension present";
        String failEvidence = "Faces Quarkus extension not found. Use either " + MYFACES_QUARKUS + " or " + QUARKUS_PRIMEFACES;
        boolean hasAnyDependency = context.buildFile().hasAnyDependency(List.of(MYFACES_QUARKUS, QUARKUS_PRIMEFACES));
        context.result(rule, hasAnyDependency, passEvidence, failEvidence);
    }

    private void checkPrimeFacesExtension(UiContext context) {
        ValidationRule rule = context.rule("primefaces_extension",
                "quarkus-primefaces must be present when PrimeFaces is used");
        ProjectFiles target = context.target();
        boolean usesPrimeFaces = target.find("", ".xhtml").stream().anyMatch(f -> target.read(f).contains("primefaces.org"))
                || target.javaFiles().stream().anyMatch(f -> target.javaSource(f).contains("org.primefaces"));
        if (!usesPrimeFaces) {
            context.pass(rule, "PrimeFaces not used");
            return;
        }
        String passEvidence = "PrimeFaces used and quarkus-primefaces present";
        String failEvidence = "PrimeFaces is used but " + QUARKUS_PRIMEFACES + " was not found";
        boolean hasDependency = context.buildFile().hasDependency(QUARKUS_PRIMEFACES);
        context.result(rule, hasDependency, passEvidence, failEvidence);
    }

    private void checkFacesServlet(UiContext context) {
        ValidationRule rule = context.rule("web_xml_faces_servlet", "META-INF/web.xml must configure the FacesServlet");
        Path webXml = context.target().resolve(WEB_XML);
        if (!Files.exists(webXml)) {
            context.fail(rule, "File not found: " + WEB_XML);
            return;
        }
        String content = XML_COMMENT.matcher(context.target().read(webXml)).replaceAll("");
        String passEvidence = "web.xml configures the FacesServlet";
        String failEvidence = "web.xml does not configure jakarta.faces.webapp.FacesServlet";
        context.result(rule, content.contains("jakarta.faces.webapp.FacesServlet"), passEvidence, failEvidence);
    }

    private void checkJakartaNamespaces(UiContext context) {
        ProjectFiles target = context.target();
        List<Path> xhtmlFiles = target.find("", ".xhtml");
        ValidationRule rule = context.rule("jakarta_faces_namespaces", "XHTML files must use Jakarta Faces namespaces");
        String passEvidence = xhtmlFiles.isEmpty() ? "No XHTML files to check" : "All XHTML files use Jakarta namespaces";
        context.offendingFiles(rule,
                xhtmlFiles, f -> OLD_FACES_NAMESPACES.stream().anyMatch(target.read(f)::contains),
                passEvidence,
                "Old namespaces");
    }

    private void checkFacesConfig(UiContext context) {
        ValidationRule rule = context.rule("faces_config_jakarta",
                "faces-config.xml must use the Jakarta Faces 4.x schema and have no Spring references");
        ProjectFiles target = context.target();
        List<Path> facesConfigs = target.find("src/main", "faces-config.xml");
        if (facesConfigs.isEmpty()) {
            context.pass(rule, "No faces-config.xml found (may be using annotations)");
            return;
        }
        List<String> problems = new ArrayList<>();
        for (Path facesConfig : facesConfigs) {
            String content = XML_COMMENT.matcher(target.read(facesConfig)).replaceAll("");
            String file = target.relative(facesConfig);
            if (!content.contains("https://jakarta.ee/xml/ns/jakartaee")) {
                problems.add(file + " does not use the Jakarta EE namespace");
            }
            Matcher version = FACES_CONFIG_VERSION.matcher(content);
            if (!version.find() || Integer.parseInt(version.group(1)) < 4) {
                problems.add(file + " must declare version 4.0 or higher");
            }
            if (content.contains("java.sun.com") || content.contains("xmlns.jcp.org")) {
                problems.add(file + " contains old JSF namespaces");
            }
            if (content.contains("SpringBeanFacesELResolver") || content.contains("org.springframework")) {
                problems.add(file + " still references Spring (e.g. SpringBeanFacesELResolver)");
            }
        }
        String passEvidence = facesConfigs.size() + " faces-config.xml file(s) use Jakarta Faces 4.x";
        String failEvidence = String.join("; ", problems);
        context.result(rule, problems.isEmpty(), passEvidence, failEvidence);
    }

    /** @ManagedBean was removed in Jakarta Faces 4, so any remaining usage is an error. */
    private void checkCdiBeans(UiContext context) {
        ProjectFiles target = context.target();
        List<String> offenders = new ArrayList<>();
        for (Path file : target.javaFiles()) {
            String source = target.javaSource(file);
            if (MANAGED_BEAN.matcher(source).find()) {
                offenders.add(target.relative(file) + " (@ManagedBean must be replaced by @Named and a CDI scope)");
            }
            if (MANAGED_PROPERTY.matcher(source).find()) {
                offenders.add(target.relative(file) + " (@ManagedProperty must be replaced by @Inject)");
            }
        }
        ValidationRule rule = context.rule("cdi_named_beans", "JSF managed beans must be converted to CDI beans");
        context.offenders(rule, offenders, "All beans use CDI annotations", "JSF managed beans");
    }

    private void checkManagedConvertersAndValidators(UiContext context) {
        ProjectFiles target = context.target();
        List<String> offenders = new ArrayList<>();
        for (Path file : target.javaFiles()) {
            String source = target.javaSource(file);
            if (!source.contains("@Inject")) {
                continue;
            }
            if (source.contains("@FacesConverter") && !MANAGED_CONVERTER.matcher(source).find()) {
                offenders.add(target.relative(file) + " (@FacesConverter with @Inject must have managed=true)");
            }
            if (source.contains("@FacesValidator") && !MANAGED_VALIDATOR.matcher(source).find()) {
                offenders.add(target.relative(file) + " (@FacesValidator with @Inject must have managed=true)");
            }
        }
        ValidationRule rule = context.rule("converter_validator_managed",
                "@FacesConverter/@FacesValidator using @Inject must set managed=true");
        String passEvidence = "All converters/validators properly configured";
        String failLabel = "Unmanaged converters/validators";
        context.offenders(rule, offenders, passEvidence, failLabel);
    }

    private void checkXhtmlLocation(UiContext context) {
        ProjectFiles target = context.target();
        List<String> misplaced = target.find(WEBAPP_DIR, ".xhtml").stream().map(target::relative).toList();
        int located = target.find(META_INF_RESOURCES, ".xhtml").size();
        ValidationRule rule = context.rule("xhtml_resources_location", "XHTML files must be in META-INF/resources");
        String passEvidence = located == 0 ? "No XHTML files found (may be expected)"
                : "XHTML files in correct location (" + located + " files in META-INF/resources)";
        String failLabel = "XHTML files in old location";
        context.offenders(rule, misplaced, passEvidence, failLabel);
    }

    private void checkSourceTargetParity(UiContext context) {
        ValidationRule rule = context.rule("source_target_parity", "All XHTML files must be migrated for JSF to MyFaces");
        int sourceCount = context.source().find("", ".xhtml").size();
        int targetCount = context.target().find(META_INF_RESOURCES, ".xhtml").size();
        String passEvidence = "XHTML file parity: " + sourceCount + " source → " + targetCount + " target";
        String failEvidence = "Missing XHTML files in target: " + sourceCount + " source files → " + targetCount
                + " target files. "
                + (sourceCount - targetCount) + " files missing.";
        context.result(rule, targetCount >= sourceCount, passEvidence, failEvidence);
    }

    private static Pattern managedPattern(String annotation) {
        return Pattern.compile(Pattern.quote(annotation) + "\\s*\\([^)]*managed\\s*=\\s*true");
    }
}
