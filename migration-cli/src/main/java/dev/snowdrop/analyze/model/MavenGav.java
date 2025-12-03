package dev.snowdrop.analyze.model;

/**
 * DTO of the Pom Dependency class used to perform the query
 */
public record MavenGav(String groupId, String artifactId, String version) {
}