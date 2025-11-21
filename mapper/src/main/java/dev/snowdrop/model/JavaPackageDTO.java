package dev.snowdrop.model;

import java.util.List;
import java.util.Set;

/**
 * DTO for Java package analysis results from JDTLS scanner.
 * Contains information about packages and their structure in Java projects.
 */
public record JavaPackageDTO(String packageName, String sourceRoot, List<String> classFiles, Set<String> imports,
		Set<String> exports, boolean isTestPackage, String moduleName) {
}