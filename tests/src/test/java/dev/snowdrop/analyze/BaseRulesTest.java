package dev.snowdrop.analyze;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class BaseRulesTest {

	protected Path rulesPath;

	public static void copyFolder(String source, Path target, CopyOption... options) throws Exception {
		URL resourceUrl = MatchServiceCookBookTest.class.getClassLoader().getResource(source);
		if (resourceUrl == null) {
			throw new RuntimeException("Resource folder not found: " + source);
		}
		Path sourcePath = Paths.get(resourceUrl.toURI());
		Path destinationPath = target;

		Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(destinationPath.resolve(sourcePath.relativize(dir).toString()));
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, destinationPath.resolve(sourcePath.relativize(file).toString()), options);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public Config createTestConfig(Path applicationToScan, Path rulesPath, String jdtls) {
		return new Config(applicationToScan.toString(), rulesPath, "springboot", "quarkus",
				Paths.get(jdtls, "konveyor-jdtls").toString(), jdtls, "io.konveyor.tackle.ruleEntry", false, "json",
				null);
	}
}
