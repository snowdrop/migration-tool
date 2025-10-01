package dev.snowdrop.commands;

import dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationAnnotationWithQuarkusMain;
import dev.snowdrop.analyze.model.MigrationTask;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import picocli.CommandLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.analyze.utils.FileUtils.resolvePath;

@CommandLine.Command(
    name = "transform",
    description = "Transform a java application"
)
public class TransformCommand implements Runnable {
    private static final Logger logger = Logger.getLogger(TransformCommand.class);

    @CommandLine.Parameters(
        index = "0",
        description = "Path to the Java project to analyze"
    )
    @ConfigProperty(name = "analyzer.app.path", defaultValue = "./applications/spring-boot-todo-app")
    public String appPath;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output"
    )
    private boolean verbose;

    @CommandLine.Option(
        names = {"-d","--dry-run"},
        description = "Execute OpenRewrite in dry-run mode (preview changes without applying them)"
    )
    private boolean dryRun;

    public static final String MAVEN_REWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
    public static final String MAVEN_REWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";
    public static final String MAVEN_REWRITE_PLUGIN_VERSION = "6.19.0";

    @Override
    public void run() {
        Path path = resolvePath(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            return;
        }
        try {
            // startTransformation();
            startNewTransformation();
        } catch (Exception e) {
            logger.errorf("❌ Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Finds the latest analysing report JSON file in the project path and load it
     *
     * @param projectPath The path to the project directory
     * @return Map of migration tasks from the latest JSON file, or empty map if no file found
     */
    private Map<String, MigrationTask> loadLatestAnalysisReport(Path projectPath) {
        try {
            Optional<Path> latestJsonFile = findLatestAnalysingReportJson(projectPath);

            if (latestJsonFile.isEmpty()) {
                logger.warnf("No analysing report JSON files found in: %s", projectPath);
                return Map.of();
            }

            Path jsonFile = latestJsonFile.get();
            logger.infof("📄 Loading migration tasks from: %s", jsonFile.getFileName());

            ObjectMapper objectMapper = new ObjectMapper();
            MigrationTasksExport export = objectMapper.readValue(jsonFile.toFile(), MigrationTasksExport.class);

            LinkedHashMap<String, MigrationTask> sortedMigrationTasks = export.migrationTasks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(task -> task.getRule().order())))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new
                ));

            return sortedMigrationTasks;

        } catch (IOException e) {
            logger.errorf("❌ Failed to load migration tasks: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return Map.of();
        }
    }

    /**
     * Finds the latest analysing report JSON file based on timestamp in filename
     *
     * @param projectPath The path to search for JSON files
     * @return Optional containing the latest JSON file path, or empty if none found
     */
    private Optional<Path> findLatestAnalysingReportJson(Path projectPath) {
        try {
            return Files.list(projectPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("analysing-report_"))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .max((path1, path2) -> {
                    try {
                        return Files.getLastModifiedTime(path1).compareTo(Files.getLastModifiedTime(path2));
                    } catch (IOException e) {
                        return path1.getFileName().toString().compareTo(path2.getFileName().toString());
                    }
                });
        } catch (IOException e) {
            logger.errorf("Error searching for JSON files: %s", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Executes OpenRewrite maven goal for a specific migration task
     *
     * @param projectPath The path to the project directory
     * @param task The migration task containing OpenRewrite instructions
     */
    private void executeOpenRewriteForTask(Path projectPath, MigrationTask task) {
        var rule = task.getRule();

        if (rule.instructions() == null || rule.instructions().openrewrite() == null) {
            logger.warnf("   ⚠️  No OpenRewrite instructions found for task, skipping");
            return;
        }

        for (var openrewrite : rule.instructions().openrewrite()) {
            if (openrewrite.recipeList() == null || openrewrite.recipeList().isEmpty()) {
                logger.warnf("   ⚠️  No recipes defined in OpenRewrite instruction, skipping");
                continue;
            }

            String compositeRecipeName = "dev.snowdrop.openrewrite.java.SpringToQuarkus";
            StringBuffer buf = new StringBuffer();
            var recipes = openrewrite.recipeList();

            if (! recipes.isEmpty()) {
                String header = """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: %s
                    displayName: %s
                    description: %s
                    """.formatted(compositeRecipeName,openrewrite.name(),openrewrite.description());
                buf.append(header);
                buf.append("recipeList: ").append("\n");

                recipes.forEach(recipe -> {
                    if (recipe instanceof Map) {
                        HashMap<String, Map<String, String>> recipeType = (HashMap<String, Map<String, String>>) recipe;
                        for (var recipeEntry : recipeType.entrySet()) {
                            String recipeName = recipeEntry.getKey();
                            Map<String, String> parameters = recipeEntry.getValue();

                            buf.append("  - ").append(recipeName).append(": ").append("\n");

                            // 2. Iterate over the inner map (Parameter Name -> Parameter Value)
                            for (var paramEntry : parameters.entrySet()) {
                                String paramName = paramEntry.getKey();
                                String paramValue = paramEntry.getValue();

                                buf.append("      ").append(paramName).append(": ").append("\"").append(paramValue).append("\"").append("\n");
                            }
                        }
                    }
                    // Check if the recipe object is a String when we process a parameter-less recipes
                    else if (recipe instanceof String) {
                        String recipeName = (String) recipe;
                        buf.append("  - ").append(recipeName).append("\n");
                    }
                });
            } else {
                throw new IllegalStateException("No recipes defined in OpenRewrite instruction, skipping");
            }
            logger.debug(buf.toString());

            String rewriteYamlName = "";
            try {
                rewriteYamlName = String.format("rewrite-%d.yml",rule.order());
                Files.write(
                    Path.of(projectPath.toString(), rewriteYamlName),
                    buf.toString().getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String gavs = String.join(",", openrewrite.gav());

            boolean success = execMvnCmd(projectPath, compositeRecipeName, gavs, rewriteYamlName);

            if (success) {
                logger.infof("   ✅ OpenRewrite execution completed successfully");
            } else {
                logger.errorf("   ❌ OpenRewrite execution failed");
            }
        }
    }

    /**
     * Executes a maven command with the specified goal and recipes
     *
     * @param projectPath The path to the project directory
     * @param compositeRecipeName Name of the composite recipe
     * @param gavs Comma-separated list of maven GAV dependencies
     * @return true if the command executed successfully, false otherwise
     */
    private boolean execMvnCmd(Path projectPath, String compositeRecipeName, String gavs, String rewriteYamlName) {
        try {
            List<String> command = new ArrayList<>();
            //String outputDirectoryRewriteName = rewriteYamlName.substring(0, rewriteYamlName.lastIndexOf('.'));
            String outputDirectoryRewriteName = "rewrite";
            logger.infof("outputDirectoryRewriteName: %s",outputDirectoryRewriteName);

            command.add("mvn");
            command.add("-B");
            command.add("-e");
            command.add(String.format("%s:%s:%s:%s", MAVEN_REWRITE_PLUGIN_GROUP, MAVEN_REWRITE_PLUGIN_ARTIFACT, MAVEN_REWRITE_PLUGIN_VERSION,
                dryRun ? "dryRun" : "run"));
            command.add("-Drewrite.activeRecipes=" + compositeRecipeName);
            command.add("-Drewrite.recipeArtifactCoordinates=" + gavs);
            command.add("-Drewrite.exportDatatables=true");
            command.add(String.format("-DreportOutputDirectory=target/%s",outputDirectoryRewriteName));
            command.add(String.format("-Drewrite.configLocation=%s",rewriteYamlName));

            logger.infof("      ==== Executing command: %s", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(projectPath.toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read and log output in real-time
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (verbose) {
                        logger.infof("      %s", line);
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            logger.errorf("   ❌ Failed to execute maven command: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private static String getMavenSettingsArg() {
        final String mavenSettings = System.getProperty("maven.settings");
        if (mavenSettings != null) {
            return Files.exists(Paths.get(mavenSettings)) ? mavenSettings : null;
        } else {
            return null;
        }
    }

    /**
     * Execute the mvn goal to apply the transformation
     *
     */
    private void startNewTransformation() {
        Instant start = Instant.now();

        Path projectPath = resolvePath(appPath);
        logger.infof("✅ Starting transformation for project at: %s", projectPath);

        Map<String, MigrationTask> migrationTasks = loadLatestAnalysisReport(projectPath);

        if (migrationTasks.isEmpty()) {
            logger.warn("❌ No migration tasks found. Please run the analyze command first to generate a migration report.");
            return;
        }

        logger.infof("📋 Found %d migration tasks to process", migrationTasks.size());

        for (Map.Entry<String, MigrationTask> entry : migrationTasks.entrySet()) {
            String taskId = entry.getKey();
            MigrationTask task = entry.getValue();

            logger.infof("🔄 Processing migration task: %s", taskId);
            if (verbose) {
                logger.infof("   Description: %s", task.getRule().description());
                logger.infof("   Category: %s", task.getRule().category());
                logger.infof("   Effort: %d", task.getRule().effort());
            }

            executeOpenRewriteForTask(projectPath, task);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logger.info("----------------------------------------");
        logger.info("--- Elapsed time: " + timeElapsed + " ms ---");
        logger.info("----------------------------------------");
    }

    /**
     * Parses all Java files and execute some recipes
     *
     */
    private void startTransformation() {
        Instant start = Instant.now();

        Path projectPath = resolvePath(appPath);
        logger.infof("✅ Starting OpenRewrite parsing for project at: %s", projectPath);

        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        // Discover all .java files in the project
        List<Path> javaFiles = discoverJavaFiles(projectPath);
        logger.infof("Found %d Java files to parse", javaFiles.size());

        if (javaFiles.isEmpty()) {
            logger.warn("No Java files found in the project");
            throw new IllegalStateException("No Java files found in the project !");
        }

        // Parse all discovered Java files
        List<J.CompilationUnit> lsts = JavaParser.fromJavaVersion()
            .build()
            .parse(javaFiles, null, ctx)
            .filter(sourceFile -> sourceFile instanceof J.CompilationUnit)
            .map(sourceFile -> (J.CompilationUnit) sourceFile)
        .toList();

        logger.infof("✅ Successfully parsed %d Java source files into LSTs.", lsts.size());

        if (verbose) {
            logger.info("--- Parsed Files ---");
            for (J.CompilationUnit cu : lsts) {
                // Each CompilationUnit has a source path that identifies the original file
                logger.infof(" -> %s", cu.getSourcePath());
            }
            logger.info("--------------------");
        }

        Recipe recipe = new ReplaceSpringBootApplicationAnnotationWithQuarkusMain();
        J.CompilationUnit transformedCu;

        for  (J.CompilationUnit cu : lsts) {
            try {
                transformedCu = (J.CompilationUnit) recipe.getVisitor().visit(cu, ctx);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }

            if (transformedCu == cu) {
                logger.info("Recipe did not make any changes.");
            } else {
                String transformedCode = transformedCu.printAll();
                logger.info("--- SOURCE CODE AFTER TRANSFORMATION ---");
                logger.info(transformedCode);
                logger.info("----------------------------------------");
            }
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logger.info("----------------------------------------");
        logger.info("--- Elapsed time: " + timeElapsed + " ms ---");
        logger.info("----------------------------------------");
    }

    /**
     * Discovers all .java files recursively in the given project directory.
     * Looks in both src/main/java and src/test/java directories.
     *
     * @param projectPath The root path of the Maven project
     * @return List of paths to .java files
     */
    private List<Path> discoverJavaFiles(Path projectPath) {
        try {
            Path mainJavaDir = projectPath.resolve("src/main/java");
            Path testJavaDir = projectPath.resolve("src/test/java");

            Stream<Path> mainJavaFiles = Files.exists(mainJavaDir)
                ? Files.walk(mainJavaDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                : Stream.empty();

            Stream<Path> testJavaFiles = Files.exists(testJavaDir)
                ? Files.walk(testJavaDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                : Stream.empty();

            return Stream.concat(mainJavaFiles, testJavaFiles).toList();

        } catch (IOException e) {
            logger.errorf("Error discovering Java files: %s", e.getMessage());
            return List.of();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MigrationTasksExport(
        String title,
        String projectPath,
        String timestamp,
        Map<String, MigrationTask> migrationTasks
    ) {}
}
