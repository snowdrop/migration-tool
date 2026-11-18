package dev.snowdrop.mtool.validate.ui;

import dev.snowdrop.mtool.project.ProjectFiles;

import java.nio.file.Path;
import java.util.List;

/**
 * Locations of the view layer files in a project, relative to the project root.
 */
final class UiPaths {

    static final String TEMPLATES_DIR = "src/main/resources/templates";
    static final String META_INF_RESOURCES = "src/main/resources/META-INF/resources";
    static final String WEBAPP_DIR = "src/main/webapp";

    private UiPaths() {
    }

    /** Qute templates of the project. */
    static List<Path> templates(ProjectFiles project) {
        return project.find(TEMPLATES_DIR, ".html");
    }
}
