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
package dev.snowdrop.openrewrite.maven.search.scanner;

import dev.snowdrop.openrewrite.maven.table.DependencyReport;
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

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = false)
@Value
@Deprecated
public class FindDependencies extends ScanningRecipe<Set<Dependency>> {

    /**
     * ID of the matching tool needed to reconcile the records where a match took place
     */
    @Option(displayName = "Match id", description = "ID of the matching tool needed to reconcile the records where a match took place", required = true)
    public String matchId;

    @Option(displayName = "Coma separated list of GAV", description = "List of Group, Artifact and Version dependencies (g:a:v) separated by coma", example = "org.springframework.boot:spring-boot-starter-web,io.jsonwebtoken:jjwt:0.9.1")
    String gavs;

    @Override
    public String getDisplayName() {
        return "Find Maven dependency";
    }

    @Override
    public String getDescription() {
        return "Finds first-order dependency uses, i.e. dependencies that are defined directly in a project.";
    }

    private transient DependencyReport report = new DependencyReport(this);

    @Override
    public Set<Dependency> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<Dependency> dependencies) {
        return new MavenIsoVisitor<ExecutionContext>() {

            List<GAV> gavList = Arrays.stream(gavs.split(",")).map(GAV::fromString) // Use the factory method
                    .collect(Collectors.toList());

            /*
             * @Override public Xml.Document visitDocument(Xml.Document d, ExecutionContext ctx) {
             * System.out.printf("XML document visited: %s",d); return d; }
             */

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                for (GAV gav : gavList) {
                    System.out.println("Processing GAV: " + gav);
                    if (isDependencyTag(gav.groupId, gav.artifactId)
                            && versionIsValid(gav.version, null, () -> findDependency(tag))) {
                        ResolvedDependency rDep = findDependency(tag);
                        System.out.printf("Dependency found: %s%n", rDep.getRequested());
                        dependencies.add(rDep.getRequested());
                        return SearchResult.found(tag);
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Set<Dependency> dependencies, ExecutionContext ctx) {
        System.out.printf("Dependencies set size: %s%n", dependencies.size());
        for (Dependency dep : dependencies) {
            report.insertRow(ctx,
                    new DependencyReport.Row(matchId, DependencyReport.Type.POM, DependencyReport.Symbol.DEPENDENCY,
                            String.format("%s:%s:%s", dep.getGroupId(), dep.getArtifactId(), dep.getVersion()),
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
        assert (validate.getValue() != null);
        return validate.getValue().isValid(actualVersion, actualVersion);
    }

    public record GAV(String groupId, String artifactId, String version) {
        public static GAV fromString(String gavString) {
            String[] parts = gavString.trim().split(":");
            if (parts.length == 2) {
                return new GAV(parts[0], parts[1], null); // Use null for missing version
            } else if (parts.length == 3) {
                return new GAV(parts[0], parts[1], parts[2]);
            }
            // Handle invalid formats
            return new GAV("invalid", "invalid", "invalid");
        }
    }
}
