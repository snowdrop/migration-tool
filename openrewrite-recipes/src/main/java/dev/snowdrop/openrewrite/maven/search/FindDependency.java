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
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindDependency extends Recipe {

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

    private transient MatchingReport report = new MatchingReport(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {

            List<GAV> gavList = Arrays.stream(gavs.split(",")).map(GAV::fromString) // Use the factory method
                    .collect(Collectors.toList());

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                for (GAV gav : gavList) {
                    if (isDependencyTag(gav.groupId, gav.artifactId)
                            && versionIsValid(gav.version, null, () -> findDependency(tag))) {
                        report.insertRow(ctx,
                                new MatchingReport.Row(matchId, MatchingReport.Type.POM,
                                        MatchingReport.Symbol.DEPENDENCY,
                                        String.format("%s:%s:%s", gav.groupId, gav.artifactId, gav.version), "pom.xml" // TODO
                                                                                                                       // :
                                                                                                                       // How
                                                                                                                       // can
                                                                                                                       // we
                                                                                                                       // get
                                                                                                                       // the
                                                                                                                       // sourceFile
                                                                                                                       // ?
                        ));
                        return SearchResult.found(tag);
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
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
