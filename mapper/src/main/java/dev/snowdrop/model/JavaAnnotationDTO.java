package dev.snowdrop.model;

import java.util.List;
import java.util.Map;

/**
 * DTO for Java annotation analysis results from OpenRewrite scanner.
 * Contains information about annotations found in Java source code.
 */
public record JavaAnnotationDTO(String annotationName, String fullyQualifiedName, String sourceFile, int lineNumber,
		String targetElement, Map<String, Object> attributes, List<String> values) {
}