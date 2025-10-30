package dev.snowdrop.transform.provider.ai;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

// import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class FileSystemTool {
    private static final Logger logger = Logger.getLogger(FileSystemTool.class.getName());

    @Tool("Reads the full content of a specified file")
    public String readFile(String path) {
        try {
            logger.info("Reading file: " + path);
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool("Writes the content to the specified file, overwriting it if it exists")
    public String writeFile(String path, String content) {

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
            Path filePath = Paths.get(path);
            logger.debugf("File path is: %s", filePath);
            Files.writeString(filePath, content);
            return "File '" + path + "' written successfully.";
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}