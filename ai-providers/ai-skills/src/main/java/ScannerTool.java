import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScannerTool {

    public ScannerTool() {}

    @Tool("Read file with line numbers (file path, not directory)")
    public String readFile(
            @P("The relative path to the file from the project root") String path,
            @P("The line offset to start reading from (0-based). Defaults to 0") String offset,
            @P("The number of lines to read. Defaults to all lines") String limit) throws IOException {

        Path target = pathNormalize(path);
        List<String> lines = Files.readAllLines(target);
        int off = (offset != null && !offset.isBlank()) ? Integer.parseInt(offset) : 0;
        int lim = (limit != null && !limit.isBlank()) ? Integer.parseInt(limit) : lines.size();
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < Math.min(off + lim, lines.size()); i++)
            sb.append(String.format("%4d| %s%n", i + 1, lines.get(i)));
        return sb.toString();
    }

    @Tool("Write content to file")
    public String writeFile(
            @P("The relative path to the file from the project root") String path,
            @P("The content to write to the file") String content) throws IOException {

        Path target = pathNormalize(path);
        Files.writeString(target, content);
        return "ok";
    }

    @Tool("Replace old with new in file (old must be unique unless all=true)")
    public String editFile(
            @P("The relative path to the file from the project root") String path,
            @P("The text to find and replace") String old,
            @P("The replacement text") String replacement,
            @P("If 'true', replace all occurrences. Defaults to false") String all) throws IOException {

        Path target = pathNormalize(path);
        String text = Files.readString(target);
        if (!text.contains(old))
            return "error: old_string not found";
        int count = (text.length() - text.replace(old, "").length()) / old.length();
        boolean replaceAll = "true".equalsIgnoreCase(all);
        if (!replaceAll && count > 1)
            return "error: old_string appears " + count + " times, must be unique (use all=true)";
        Files.writeString(target, replaceAll
                ? text.replace(old, replacement)
                : text.replaceFirst(Pattern.quote(old), Matcher.quoteReplacement(replacement)));
        return "ok";
    }

    @Tool("Find files by glob pattern, sorted by modification time")
    public String globFiles(
            @P("The glob pattern (e.g., '**/*.java', '*.xml')") String pattern,
            @P("The relative directory to search from (use '.' for root). Defaults to '.'") String path)
            throws IOException {

        String dir = (path != null && !path.isBlank()) ? path : ".";
        Path base = pathNormalize(dir);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + base + "/" + pattern);
        if (!Files.exists(base))
            return "none";
        try (Stream<Path> walk = Files.walk(base)) {
            List<String> files = walk.filter(Files::isRegularFile)
                    .filter(matcher::matches)
                    .sorted((Path a, Path b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .map(p -> base.relativize(p).toString())
                    .collect(Collectors.toList());
            return files.isEmpty() ? "none" : String.join("\n", files);
        }
    }

    @Tool("Recursively searches for a regex pattern within the project files")
    public String grepProject(
            @P("The text or regex pattern to search for (e.g., '@SpringBootApplication')") String pattern,
            @P("The relative directory to start the search from (use '.' for everywhere)") String startDir)
            throws IOException {

        Path startPath = pathNormalize(startDir);
        Pattern compiled = Pattern.compile(pattern);
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(startPath)) {
            List<Path> files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            for (Path file : files) {
                if (isBlacklisted(file))
                    continue;
                if (hits.size() >= 50)
                    break;
                try {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size() && hits.size() < 50; i++)
                        if (compiled.matcher(lines.get(i)).find())
                            hits.add(startPath.relativize(file) + ":" + (i + 1) + ": " + lines.get(i).trim());
                } catch (Exception e) {
                    /* skip binary or unreadable files */
                }
            }
        }
        return hits.isEmpty() ? "none" : String.join("\n", hits);
    }

    @Tool("Run shell command")
    public String bash(
            @P("The shell command to execute") String command) throws Exception {

        Process proc = new ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start();
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null)
                out.add(line);
        }
        if (!proc.waitFor(30, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            out.add("(timed out after 30s)");
        }
        return out.isEmpty() ? "(empty)" : String.join("\n", out);
    }

    /**
     * Normalize the path of the AI's tool request
     */
    private Path pathNormalize(String path) {
        return Path.of(path).normalize();
    }

    /**
     * Prevents the AI from wasting cycles on target/ or .git/ folders.
     */
    private boolean isBlacklisted(Path path) {
        String p = path.toString();
        return p.contains("/target/") || p.contains("/.git/") || p.contains("/.idea/") || p.endsWith(".class");
    }
}
