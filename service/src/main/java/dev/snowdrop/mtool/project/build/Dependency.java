package dev.snowdrop.mtool.project.build;

/**
 * Coordinates of a build dependency. An {@link #ANY_ARTIFACT} artifactId matches every artifact of the group.
 */
public record Dependency(String groupId, String artifactId) {

    public static final String ANY_ARTIFACT = "*";

    public static Dependency of(String groupId, String artifactId) {
        return new Dependency(groupId, artifactId);
    }

    public static Dependency anyOf(String groupId) {
        return new Dependency(groupId, ANY_ARTIFACT);
    }

    public boolean anyArtifact() {
        return ANY_ARTIFACT.equals(artifactId);
    }

    @Override
    public String toString() {
        return anyArtifact() ? groupId : groupId + ":" + artifactId;
    }
}
