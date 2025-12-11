package dev.snowdrop.analyze.model;

import java.nio.file.Path;

/**
 * Represents a specific pattern occurrence found within a file.
 *
 * @param filePath The path to the file containing the match.
 * @param lineNumber The 1-based line number where the match occurred.
 * @param lineContent The content of the entire line where the match was found.
 * @param matchedText The exact text chunk that matched the pattern.
 */
public record MatchLocation(Path filePath, int lineNumber, String lineContent, String matchedText) {
}
