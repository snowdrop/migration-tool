package dev.snowdrop.mtool.project;

import dev.snowdrop.mtool.project.build.BuildFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index of the files of a project, built with a single walk of the project tree. File contents are read
 * once and cached, so that the checks can inspect the same files without walking or reading them again.
 */
public final class ProjectFiles {

    public static final String JAVA_DIR = "src/main/java";
    public static final String RESOURCES_DIR = "src/main/resources";

    /** Directories that never contain project sources. */
    private static final Set<String> EXCLUDED_DIRS = Set.of(".git", ".m2", ".gradle", ".idea", "node_modules");
    /** Build output directories, only excluded next to a build file so that e.g. a "build" package is kept. */
    private static final Set<String> BUILD_OUTPUT_DIRS = Set.of("target", "build");
    private static final List<String> BUILD_FILES = List.of("pom.xml", "build.gradle", "build.gradle.kts");

    private final Path root;
    private final List<Path> files;
    private final Map<Path, String> contents = new HashMap<>();
    private final Map<Path, String> javaSources = new HashMap<>();
    private BuildFile buildFile;
    private ApplicationConfig applicationConfig;

    private ProjectFiles(Path root, List<Path> files) {
        this.root = root;
        this.files = files;
    }

    /**
     * @throws UncheckedIOException when the tree cannot be walked, since reporting "no files found"
     *         would turn a scan error into a passing check
     */
    public static ProjectFiles scan(Path root) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(absoluteRoot)) {
            return new ProjectFiles(absoluteRoot, List.of());
        }
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(absoluteRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return isExcluded(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not scan " + absoluteRoot, e);
        }
        files.sort(null);
        return new ProjectFiles(absoluteRoot, List.copyOf(files));
    }

    private static boolean isExcluded(Path dir) {
        String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
        if (EXCLUDED_DIRS.contains(name)) {
            return true;
        }
        Path parent = dir.getParent();
        return BUILD_OUTPUT_DIRS.contains(name) && parent != null
                && BUILD_FILES.stream().anyMatch(f -> Files.exists(parent.resolve(f)));
    }

    public Path root() {
        return root;
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath);
    }

    public boolean isDirectory(String relativePath) {
        return Files.isDirectory(resolve(relativePath));
    }

    /** Files under the given directory (relative to the project root, "" for the root) with one of the suffixes. */
    public List<Path> find(String relativeDir, String... suffixes) {
        return find(resolve(relativeDir), Arrays.asList(suffixes));
    }

    public List<Path> find(String relativeDir, List<String> suffixes) {
        return find(resolve(relativeDir), suffixes);
    }

    private List<Path> find(Path dir, List<String> suffixes) {
        return files.stream()
                .filter(f -> f.startsWith(dir))
                .filter(f -> suffixes.stream().anyMatch(s -> f.getFileName().toString().endsWith(s)))
                .toList();
    }

    public List<Path> javaFiles() {
        return find(JAVA_DIR, ".java");
    }

    /** True when a file named {@code <name>.<extension>} exists directly in the given directory. */
    public boolean hasFileNamed(Path dir, String name) {
        return files.stream().anyMatch(f -> dir.equals(f.getParent())
                && f.getFileName().toString().startsWith(name + "."));
    }

    public String read(Path file) {
        return contents.computeIfAbsent(file, TextFiles::read);
    }

    /** Content of a Java file with its comments removed. */
    public String javaSource(Path file) {
        return javaSources.computeIfAbsent(file, f -> JavaSources.stripComments(read(f)));
    }

    public String relative(Path file) {
        return root.relativize(file).toString();
    }

    public ApplicationConfig applicationConfig() {
        if (applicationConfig == null) {
            applicationConfig = ApplicationConfig.load(this);
        }
        return applicationConfig;
    }

    public BuildFile buildFile() {
        if (buildFile == null) {
            buildFile = BuildFile.load(root, find("", BUILD_FILES));
        }
        return buildFile;
    }
}
