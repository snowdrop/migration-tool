package dev.snowdrop.analyze.services.scanners;

import dev.snowdrop.analyze.model.FilePath;
import dev.snowdrop.analyze.model.MatchLocation;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Utility class for searching files based on their name or path pattern,
 * and searching content within those files.
 */
public class FileSearch {

	/**
	 * Finds application configuration files based on the specified file type.
	 *
	 * @param startPath The root directory to begin the search from.
	 * @param fileType  The type of configuration files to search for:
	 *                  "properties" - searches for application.properties, application.yml, application.yaml
	 *                  "properties.dev" - searches for application-dev.properties, application-dev.yml, application-dev.yaml
	 * @return A list of FilePath objects for all matching configuration files.
	 */
	public List<FilePath> findPropertiesPaths(Path startPath, String fileType) {
		String pattern = getGlobPatternByFileType(fileType);
		return findPaths(startPath, pattern, false);
	}

	/**
	 * Searches for files or folders matching a given pattern (glob or regex).
	 *
	 * @param startPath          The root directory to begin the search from.
	 * @param pattern            The pattern string (e.g., "glob:*.properties" or "regex:application\\.(yml|yaml|properties)$").
	 * @param includeDirectories If true, directories matching the pattern are included in results.
	 * @return A list of FilePath objects for all paths matching the criteria.
	 */
	public List<FilePath> findPaths(Path startPath, String pattern, boolean includeDirectories) {
		List<FilePath> results = new ArrayList<>();

		try {
			// PathMatcher correctly handles the "glob:" or "regex:" prefix for file system patterns.
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);

			try (Stream<Path> pathStream = Files.walk(startPath)) {
				pathStream.forEach(filePath -> {
					boolean isDirectory = Files.isDirectory(filePath);
					if (!includeDirectories && isDirectory) {
						return;
					}

					// Match against the path relative to the start path.
					Path relativePath = startPath.relativize(filePath);

					if (matcher.matches(relativePath)) {
						results.add(new FilePath(filePath, isDirectory));
					}
				});
			} catch (IOException e) {
				System.err.println("Error walking file tree: " + e.getMessage());
			}
		} catch (PatternSyntaxException e) {
			System.err.println("Invalid pattern syntax: " + e.getMessage());
		}

		return results;
	}

	/**
	 * Recursively searches for a pattern in files filtered using a glob or regex pattern.
	 *
	 * @param startPath               The root directory to start the search from.
	 * @param filePattern             Accepts glob: or regex: for file/path filtering
	 * @param pattern                 Pure regex to search for within the file content (e.g., "@RequestMapping").
	 * @return    A list of MatchLocation for all occurrences found.
	 */
	public List<MatchLocation> findMatches(Path startPath, String filePattern, String pattern) {
		List<MatchLocation> results = new ArrayList<>();

		// Filter the files using the filePatter,
		List<FilePath> targetFiles = findPaths(startPath, filePattern, false);

		Pattern contentPattern = Pattern.compile(pattern);

		// Iterate over the filtered files and search the content
		targetFiles.stream().map(FilePath::filePath).forEach(filePath -> {
			results.addAll(searchInFile(filePath, contentPattern));
		});

		return results;
	}

	/**
	 * Recursively searches for a pattern in properties files based on the specified file type.
	 *
	 * @param startPath The root directory to start the search from.
	 * @param fileType  The type of configuration files to search in:
	 *                  "properties" - searches in application.properties, application.yml, application.yaml
	 *                  "properties.dev" - searches in application-dev.properties, application-dev.yml, application-dev.yaml
	 * @param pattern   Pure regex to search for within the file content (e.g., "server\\.port", "spring\\.datasource").
	 * @return A list of MatchLocation for all occurrences found in the properties files.
	 */
	public List<MatchLocation> findPropertiesMatches(Path startPath, String fileType, String pattern) {
		String filePattern = getGlobPatternByFileType(fileType);
		return findMatches(startPath, filePattern, pattern);
	}

	/**
	 * Returns the appropriate glob pattern for the specified file type.
	 *
	 * @param fileType The type of configuration files to get pattern for.
	 * @return The glob pattern string for the specified file type.
	 * @throws IllegalArgumentException if the file type is not supported.
	 */
	private String getGlobPatternByFileType(String fileType) {
		switch (fileType.toLowerCase()) {
			case "properties" :
				return "glob:**/resources/{application.properties,application.yml,application.yaml}";
			case "properties.dev" :
				return "glob:**/resources/{application-dev.properties,application-dev.yml,application-dev.yaml}";
			default :
				throw new IllegalArgumentException(
						"Unsupported file type: " + fileType + ". Supported types are: 'properties', 'properties.dev'");
		}
	}

	/**
	 * Searches all occurrences of a pattern in a single file.
	 */
	private List<MatchLocation> searchInFile(Path filePath, Pattern pattern) {
		List<MatchLocation> matches = new ArrayList<>();
		int lineNumber = 0;

		try (Stream<String> lines = Files.lines(filePath)) {
			// Iterate over each line
			for (String line : (Iterable<String>) lines::iterator) {
				lineNumber++;
				Matcher matcher = pattern.matcher(line);

				// Find all matches within the current line
				while (matcher.find()) {
					matches.add(new MatchLocation(filePath, lineNumber, line.trim(), matcher.group() // The exact matched text
					));
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading file " + filePath + ": " + e.getMessage());
		}
		return matches;
	}
}