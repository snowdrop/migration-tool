package dev.snowdrop.model;

import java.util.Map;

/**
 * DTO of the Pom Dependency class used to perform the query
 */
public record MavenDependencyDTO(String groupId, String artifactId, String version, Map cmdParams) {
}