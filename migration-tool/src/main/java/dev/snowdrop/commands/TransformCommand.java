package dev.snowdrop.commands;

import dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationAnnotationWithQuarkusMain;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

    @Override
    public void run() {
        Path path = resolvePath(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            return;
        }
        try {
            startTransformation();
        } catch (Exception e) {
            logger.errorf("❌ Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }

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
}
