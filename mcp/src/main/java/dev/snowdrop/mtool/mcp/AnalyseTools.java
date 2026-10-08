package dev.snowdrop.mtool.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

public class AnalyseTools {

    private static final List<String> EXTENSIONS = List.of(
            ".java", ".properties", ".yaml", ".yml", ".xml", ".json");

    @Tool(description = "Analyze the source to discover the java classes, properties, etc files")
    public String sourceAnalyze(@ToolArg(description = "Project path", defaultValue = ".") String projectPath) {
        Path root = Path.of(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return "Error: directory not found: " + root;
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<String> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> EXTENSIONS.stream().anyMatch(ext -> p.toString().endsWith(ext)))
                    .filter(p -> !p.toString().contains("/target/"))
                    .map(root::relativize)
                    .sorted()
                    .map(Path::toString)
                    .collect(Collectors.toList());

            if (files.isEmpty()) {
                return "No source files found under: " + root;
            }

            return "Found " + files.size() + " file(s) under " + root + ":\n"
                    + String.join("\n", files);
        } catch (IOException e) {
            return "Error walking directory " + root + ": " + e.getMessage();
        }
    }
}
