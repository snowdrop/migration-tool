package dev.snowdrop.mtool.scanner.analyzer;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface JavaAnalyzer {

    Logger LOG = Logger.getLogger(JavaAnalyzer.class);

    Set<String> EXCLUDED_DIRS = Set.of(
            "target", "build", ".git", ".svn", "node_modules", ".m2");

    /**
     * @deprecated Use {@link #extractTypes(Path)} instead, which returns structured
     *             {@link JavaTypeDeclaration} records with {@link dev.snowdrop.mtool.model.analyze.AnnotationInfo}
     *             instead of raw annotation strings.
     */
    @Deprecated
    AnalysisResult analyze(Path projectPath);

    List<JavaTypeDeclaration> extractTypes(Path projectPath);

    default List<Path> findJavaFiles(Path projectPath) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        for (Path part : projectPath.relativize(p)) {
                            if (EXCLUDED_DIRS.contains(part.toString())) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .forEach(files::add);
        } catch (IOException e) {
            LOG.errorf("Error walking project tree: %s", e.getMessage());
        }
        return files;
    }
}
