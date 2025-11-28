package dev.snowdrop.scanner.file.search.model;

import java.nio.file.Path;

/**
 * Represents the location of a file or directory found during a file search.
 * This is simpler than MatchLocation as it does not include line number or content.
 *
 * @param filePath The absolute path to the file or directory.
 * @param isDirectory Flag indicating if the path is a directory.
 */
public record FilePath(Path filePath, boolean isDirectory) {
}
