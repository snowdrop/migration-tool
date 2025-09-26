package dev.snowdrop.analyze.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());
    private static Path tempDir;

    public static Path getApplicationDir(String applicationPath) {
        return Paths.get(System.getProperty("user.dir"),applicationPath);
    }

    public static Path getExampleDir() {
        return Paths.get(System.getProperty("user.dir"),"example");
    }

    public static Path getTempDir() throws IOException {
        if (tempDir == null) {
            tempDir = Files.createTempDirectory("lsp");
        }
        logger.infof("Project path: %s", tempDir.toString());
        return tempDir;
    }

    public static Path resolvePath(String pathString) {
        logger.debugf("📋 Resolving path: %s", pathString);

        if (pathString == null) {
            throw new IllegalArgumentException("Path string cannot be null");
        }

        Path path = Paths.get(pathString);
        if (path.isAbsolute()) {
            logger.debugf("📋 Path is already absolute: %s", path);
            return path;
        } else {
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            Path normalizedAndAbsPath = currentDir.resolve(pathString).normalize().toAbsolutePath();
            logger.debugf("📋 Resolved relative path '%s' to: %s", pathString, normalizedAndAbsPath);
            return normalizedAndAbsPath;
        }
    }
}
