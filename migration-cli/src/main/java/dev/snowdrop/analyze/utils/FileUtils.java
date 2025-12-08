package dev.snowdrop.analyze.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
	private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

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
