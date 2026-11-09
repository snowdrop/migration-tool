package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.project.ApplicationConfig;

import java.util.Optional;

import static dev.snowdrop.mtool.project.ProjectFiles.RESOURCES_DIR;

/**
 * Default template set of the {@code quarkus-freemarker} extension.
 *
 * When neither {@code quarkus.freemarker.base-path} nor {@code quarkus.freemarker.includes} is set, the
 * extension loads the templates from {@code freemarker/templates} with the {@code **} glob. Once one of them
 * is set, a missing {@code base-path} means the resources root, and a missing {@code includes} fails the build.
 */
record FreeMarkerTemplateSet(Optional<String> basePath, Optional<String> includes) {

    static final String BASE_PATH = "quarkus.freemarker.base-path";
    static final String INCLUDES = "quarkus.freemarker.includes";
    static final String DEFAULT_BASE_PATH = "freemarker/templates";

    static FreeMarkerTemplateSet of(ApplicationConfig config) {
        return new FreeMarkerTemplateSet(config.get(BASE_PATH), config.get(INCLUDES));
    }

    /** True when the project relies on the extension defaults. */
    boolean isDefault() {
        return basePath.isEmpty() && includes.isEmpty();
    }

    /** {@code base-path} set without {@code includes}: the extension fails the build. */
    boolean isMissingIncludes() {
        return basePath.isPresent() && includes.isEmpty();
    }

    /** Directory of the templates, relative to the project root. */
    String templatesDir() {
        String base = isDefault() ? DEFAULT_BASE_PATH : basePath.orElse("");
        return base.isEmpty() ? RESOURCES_DIR : RESOURCES_DIR + "/" + base.replaceAll("^/+|/+$", "");
    }
}
