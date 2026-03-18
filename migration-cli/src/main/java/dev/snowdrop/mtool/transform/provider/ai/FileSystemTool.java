package dev.snowdrop.mtool.transform.provider.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A LangChain4j tool that exposes file system read and write operations to AI agents.
 *
 * <p>This CDI bean is registered as a tool provider for the AI migration provider. It allows a
 * large language model to read source files for analysis and write back modified content as part of
 * an AI-driven migration task.
 *
 * <p>All file paths are resolved relative to a configurable {@link #setBasePath(Path) base path},
 * which is typically set to the root of the project being migrated. Absolute paths are used as-is.
 */
@ApplicationScoped
public class FileSystemTool {
	private static final Logger logger = Logger.getLogger(FileSystemTool.class.getName());

	private Path basePath = Path.of(".");

	/**
	 * Sets the base directory used to resolve relative file paths.
	 *
	 * <p>Should be called before the tool is used by the AI agent, typically set to the root of
	 * the application being migrated.
	 *
	 * @param basePath the directory to use as the resolution root for relative paths
	 */
	public void setBasePath(Path basePath) {
		this.basePath = basePath;
	}

	private Path resolve(String path) {
		Path p = Paths.get(path);
		return p.isAbsolute() ? p : basePath.resolve(p);
	}

	/**
	 * Reads the full content of a file at the given path.
	 *
	 * <p>Relative paths are resolved against the configured {@link #setBasePath(Path) base path}.
	 * This tool is exposed to the AI agent so it can inspect source files before proposing changes.
	 *
	 * @param path path to the file to read; may be absolute or relative to the base path
	 * @return the file content as a string, or an error message if the file cannot be read
	 */
	@Tool("Reads the full content of a specified file")
	public String readFile(@P("Path to the file to analyze") String path) {
		try {
			Path resolved = resolve(path);
			logger.debugf("Reading file: %s", resolved);
			return Files.readString(resolved);
		} catch (IOException e) {
			return "Error reading file: " + e.getMessage();
		}
	}

	/**
	 * Writes content to the specified file, overwriting it if it already exists.
	 *
	 * <p>Relative paths are resolved against the configured {@link #setBasePath(Path) base path}.
	 * This tool is exposed to the AI agent so it can persist the code changes it proposes during a
	 * migration task.
	 *
	 * @param path    path to the file to write; may be absolute or relative to the base path; must
	 *                not be null or blank
	 * @param content the content to write to the file; must not be null
	 * @return a success message if the file was written, or an error message if the path/content
	 *         are invalid or an I/O error occurs
	 */
	@Tool("Writes the content to the specified file, overwriting it if it exists")
	public String writeFile(@P("Path to the file where the content must be changed") String path,
			@P("The content that you AI proposes to change") String content) {

		logger.debugf("Writing file: %s", path);
		logger.debugf("String content : %s", content);

		if (path == null || path.isBlank() || content == null) {
			return "Error: File path and content cannot be null or empty. Please provide both.";
		}

		/*
		 * Console console = System.console(); if (console == null) { return
		 * "Confirmation failed: Console not available."; }
		 *
		 * logger.info("\n--------------------------------------------------");
		 * logger.info("AI is requesting to WRITE to the file: " + path); logger.info("Content to be written:");
		 * logger.info(content); logger.info("--------------------------------------------------"); String confirmation
		 * = console.readLine("Do you want to proceed? (y/n): ");
		 *
		 * if (!"y".equalsIgnoreCase(confirmation)) { return "Write operation cancelled by user."; }
		 */

		try {
			Path filePath = resolve(path);
			logger.debugf("File path is: %s", filePath);
			Files.writeString(filePath, content);
			return "File '" + path + "' written successfully.";
		} catch (IOException e) {
			return "Error writing file: " + e.getMessage();
		}
	}
}