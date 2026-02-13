package dev.snowdrop.mtool.tests.analyze;

import dev.snowdrop.mtool.model.analyze.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;

public class BaseRulesTest {

	protected Path rulesPath;

	public static void copyFolder(String source, Path target, CopyOption... options) throws Exception {
		URL resourceUrl = BaseRulesTest.class.getClassLoader().getResource(source);
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

	public Config createTestConfig(Path applicationToScan, Path rulesPath) {
		return new Config(applicationToScan.toString(), rulesPath, "springboot", "quarkus", "", "",
				"io.konveyor.tackle.ruleEntry", false, "json", null, "6.29.0");
	}

	public Config createTestConfig(Path applicationToScan, Path rulesPath, String jdtls) {
		return new Config(applicationToScan.toString(), rulesPath, "springboot", "quarkus",
				Paths.get(jdtls, "konveyor-jdtls").toString(), jdtls, "io.konveyor.tackle.ruleEntry", false, "json",
				null, "6.29.0");
	}

	public static void runCat(Path pathToFile) throws Exception {
		if (Files.exists(pathToFile)) {
			ProcessBuilder pb = new ProcessBuilder("cat", pathToFile.toString());
			Process process = pb.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String output = reader.lines().collect(Collectors.joining("\n"));

				int exitCode = process.waitFor();
				if (exitCode != 0) {
					throw new RuntimeException("Command failed with code " + exitCode);
				}
				System.out.printf("### Cat %s: %s\n", pathToFile, output);
			}
		} else {
			System.out.println("### rewrite.patch file don't exists\n");
		}
	}
}
