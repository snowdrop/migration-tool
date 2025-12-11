package dev.snowdrop.analyze;

import java.nio.file.Path;

public record Config(String appPath, Path rulesPath, String sourceTechnology, String targetTechnology, String jdtLsPath,
		String jdtWks, String lsCmd, boolean verbose, String output, String scanner) {
}
