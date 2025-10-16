/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.snowdrop.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindDependency extends ScanningRecipe<Set<Dependency>> {

    /**
     * ID of the matching tool needed to reconcile the records where a match took place
     */
    @Option(displayName = "Match id",
        description = "ID of the matching tool needed to reconcile the records where a match took place",
        required = true)
    public String matchId;

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.0.0",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Find Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        String maybeVersionSuffix = version == null ? "" : String.format(":%s%s", version, versionPattern == null ? "" : versionPattern);
        return String.format("`%s:%s%s`", groupId, artifactId, maybeVersionSuffix);
    }

    @Override
    public String getDescription() {
        return "Finds first-order dependency uses, i.e. dependencies that are defined directly in a project.";
    }

    private final MatchingReport report = new MatchingReport(this);

    public static Set<Xml.Tag> find(Xml.Document maven, String groupId, String artifactId) {
        return find(maven, groupId, artifactId, null, null);
    }

    public static Set<Xml.Tag> find(Xml.Document maven, String groupId, String artifactId,
                                    @Nullable String version, @Nullable String versionPattern) {
        Set<Xml.Tag> ds = new HashSet<>();
        new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId) &&
                    versionIsValid(version, versionPattern, () -> findDependency(tag))) {
                    ds.add(tag);
                }
                return super.visitTag(tag, ctx);
            }
        }.visit(maven, new InMemoryExecutionContext());
        return ds;
    }

    @Override
    public Set<Dependency> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<Dependency> dependencies) {
        return new MavenIsoVisitor<ExecutionContext>() {

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId) &&
                    versionIsValid(version, versionPattern, () -> findDependency(tag))) {
                    ResolvedDependency rDep = findDependency(tag);
                    System.out.printf("Dependency found: %s%n", rDep.getRequested().toString());
                    dependencies.add(rDep.getRequested());
                    return tag;
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Set<Dependency> dependencies, ExecutionContext ctx) {
        for (Dependency dep : dependencies) {
            report.insertRow(ctx,new MatchingReport.Row(
                matchId,
                MatchingReport.Type.POM,
                MatchingReport.Symbol.DEPENDENCY,
                String.format("%s:%s:%s",dep.getGroupId(),dep.getArtifactId(),dep.getVersion()),
                "pom.xml" // TODO : How can we get the sourceFile ?
            ));

        }

        return emptyList();
    }

    private static boolean versionIsValid(@Nullable String desiredVersion, @Nullable String versionPattern,
                                          Supplier<@Nullable ResolvedDependency> resolvedDependencySupplier) {
        if (desiredVersion == null) {
            return true;
        }
        ResolvedDependency resolvedDependency = resolvedDependencySupplier.get();
        if (resolvedDependency == null) {
            // shouldn't happen, but if it does, fail the condition
            return false;
        }
        String actualVersion = resolvedDependency.getVersion();
        Validated<VersionComparator> validate = Semver.validate(desiredVersion, versionPattern);
        if (validate.isInvalid()) {
            return false;
        }
        assert(validate.getValue() != null);
        return validate.getValue().isValid(actualVersion, actualVersion);
    }
}
