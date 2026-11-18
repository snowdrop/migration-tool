package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.project.build.Dependency;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Supported UI migration paths for Spring to Quarkus migrations, with the data describing the view
 * technology each one migrates from.
 */
public enum UiMigrationType {

    JSP_QUTE("jsp-qute", Target.QUTE,
            List.of(".jsp", ".jspx"),
            List.of(Dependency.of("javax.servlet", "jsp-api"),
                    Dependency.of("jakarta.servlet.jsp", "jakarta.servlet.jsp-api"),
                    Dependency.of("javax.servlet.jsp.jstl", "jstl"),
                    Dependency.of("jakarta.servlet.jsp.jstl", "jakarta.servlet.jsp.jstl-api"),
                    Dependency.anyOf("org.apache.taglibs"),
                    Dependency.of("org.apache.tomcat.embed", "tomcat-embed-jasper")),
            List.of(Pattern.compile("\\$\\{[^}]+\\}"))),

    THYMELEAF_QUTE("thymeleaf-qute", Target.QUTE,
            List.of(".html"),
            List.of(Dependency.of("org.springframework.boot", "spring-boot-starter-thymeleaf"),
                    Dependency.of("org.thymeleaf", "thymeleaf"),
                    Dependency.of("org.thymeleaf", "thymeleaf-spring5"),
                    Dependency.of("org.thymeleaf", "thymeleaf-spring6"),
                    Dependency.anyOf("org.thymeleaf.extras"),
                    Dependency.of("io.quarkus", "quarkus-thymeleaf")),
            List.of(Pattern.compile("\\$\\{[^}]+\\}"),
                    Pattern.compile("\\*\\{[^}]+\\}"),
                    Pattern.compile("@\\{[^}]+\\}"),
                    Pattern.compile("#\\{[^}]+\\}"))),

    FREEMARKER_QUTE("freemarker-qute", Target.QUTE,
            FreeMarker.EXTENSIONS,
            List.of(FreeMarker.SPRING_STARTER, FreeMarker.ENGINE, FreeMarker.QUARKUS_EXTENSION),
            List.of(Pattern.compile("\\$\\{[^}]+\\}"))),

    FREEMARKER_QUARKUS("freemarker-quarkus", Target.FREEMARKER,
            FreeMarker.EXTENSIONS,
            List.of(FreeMarker.SPRING_STARTER, FreeMarker.ENGINE),
            List.of()),

    JSF_QUTE("jsf-qute", Target.QUTE,
            List.of(".xhtml"),
            List.of(Dependency.of("jakarta.faces", "jakarta.faces-api"),
                    Dependency.of("org.glassfish", "jakarta.faces"),
                    Dependency.of("org.apache.myfaces.core", "myfaces-api"),
                    Dependency.of("org.apache.myfaces.core", "myfaces-impl"),
                    Dependency.of("org.primefaces", "primefaces"),
                    Dependency.of("org.omnifaces", "omnifaces"),
                    Dependency.anyOf("org.joinfaces")),
            List.of(Pattern.compile("#\\{[^}]+\\}"))),

    JSF_MYFACES("jsf-myfaces", Target.MYFACES,
            List.of(".xhtml"),
            List.of(),
            List.of());

    /** View technology of the migrated application. */
    enum Target {
        QUTE,
        FREEMARKER,
        MYFACES
    }

    private final String value;
    private final Target target;
    private final List<String> sourceViewExtensions;
    private final List<Dependency> legacyDependencies;
    private final List<Pattern> legacyExpressions;

    UiMigrationType(String value, Target target, List<String> sourceViewExtensions,
            List<Dependency> legacyDependencies, List<Pattern> legacyExpressions) {
        this.value = value;
        this.target = target;
        this.sourceViewExtensions = sourceViewExtensions;
        this.legacyDependencies = legacyDependencies;
        this.legacyExpressions = legacyExpressions;
    }

    public String getValue() {
        return value;
    }

    /** True for the migrations whose target view technology is Qute. */
    public boolean isQute() {
        return target == Target.QUTE;
    }

    /** Extensions of the view files of the source project. */
    List<String> sourceViewExtensions() {
        return sourceViewExtensions;
    }

    /** Dependencies of the source view technology that must be removed from the target project. */
    List<Dependency> legacyDependencies() {
        return legacyDependencies;
    }

    /**
     * Expression language of the source view technology that must not remain in Qute templates. Tags and
     * directives (JSTL, FreeMarker, JSF components) are covered by the technology removal checks.
     */
    List<Pattern> legacyExpressions() {
        return legacyExpressions;
    }

    public static UiMigrationType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("UI migration type cannot be null. Valid types: " + validTypes());
        }
        return Arrays.stream(values())
                .filter(t -> t.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid UI migration type: '" + value + "'. Valid types: " + validTypes()));
    }

    /**
     * Parses a comma-separated list of migration types (e.g. "jsp-qute,thymeleaf-qute"),
     * for projects that mix several view technologies.
     */
    public static Set<UiMigrationType> fromValues(String values) {
        Set<UiMigrationType> types = EnumSet.noneOf(UiMigrationType.class);
        if (values != null) {
            Arrays.stream(values.split(","))
                    .filter(v -> !v.isBlank())
                    .map(UiMigrationType::fromValue)
                    .forEach(types::add);
        }
        if (types.isEmpty()) {
            throw new IllegalArgumentException("UI migration type cannot be empty. Valid types: " + validTypes());
        }
        return types;
    }

    public static String validTypes() {
        return Arrays.stream(values()).map(UiMigrationType::getValue).collect(Collectors.joining(", "));
    }

    /** Values shared by the two FreeMarker paths; an enum constant cannot reference a static field of its enum. */
    private static final class FreeMarker {
        static final List<String> EXTENSIONS = List.of(".ftl", ".ftlh", ".ftlx");
        static final Dependency SPRING_STARTER = Dependency.of("org.springframework.boot",
                "spring-boot-starter-freemarker");
        static final Dependency ENGINE = Dependency.of("org.freemarker", "freemarker");
        static final Dependency QUARKUS_EXTENSION = Dependency.of("io.quarkiverse.freemarker", "quarkus-freemarker");
    }
}
