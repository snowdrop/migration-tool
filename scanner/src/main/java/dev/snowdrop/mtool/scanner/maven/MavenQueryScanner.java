package dev.snowdrop.mtool.scanner.maven;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Result;
import dev.snowdrop.mtool.model.analyze.MavenGav;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.scanner.QueryScanner;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Scanner implementation for Maven POM dependency queries. Handles queries like pom.dependency.
 */
public class MavenQueryScanner implements QueryScanner {
    private static final Logger logger = Logger.getLogger(MavenQueryScanner.class);
    private ModelBuilder modelBuilder = null;

    public MavenQueryScanner() {
    }

    @Deprecated
    @Override
    public List<Result> executeQueries(Config config, Set<Query> queries) {
        logger.infof("Maven scanner executing %d queries", queries.size());

        List<Result> allResults = new ArrayList<>();

        for (Query q : queries) {
            List<Result> partial = scansCodeFor(config, q);

            if (partial != null && !partial.isEmpty()) {
                allResults.addAll(partial);
            }
        }

        logger.infof("Maven scanner completed. Total matches found: %d", allResults.size());
        return allResults;
    }

    @Override
    public List<Result> scansCodeFor(Config config, Query query) {
        logger.infof("Maven scanner executing for query %s.%s", query.fileType(), query.symbol());

        List<Result> results = executeSingleQuery(config, query);

        logger.debugf("Found %d matches for query %s.%s ", results.size(), query.fileType(), query.symbol());

        logger.infof("Maven scanner completed. Total matches found: %d", results.size());
        return results;
    }

    @Override
    public String getScannerType() {
        return "maven";
    }

    @Override
    public boolean supports(Query query) {
        // Check the configuration to see if this query should use the Maven scanner
        String symbol = query.symbol();
        String fileType = query.fileType();
        return (fileType.contains("pom") && symbol.contains("dependency")
                || fileType.equals("pom") && symbol.equals("dependencies"));
    }

    private List<Result> executeSingleQuery(Config config, Query query) {
        List<Result> results = new ArrayList<>();

        logger.infof("Executing Maven dependency query: %s", query);

        // Find the pom.xml path
        Path pomPath = Paths.get(config.appPath(), "pom.xml");

        // No GAVs defined, so let find all the dependencies
        if (query.keyValues().isEmpty()) {
            List<Dependency> dependencies = findDependencies(pomPath.toString());
            dependencies.forEach(l -> {
                var g = l.getGroupId();
                var a = l.getArtifactId();
                var v = l.getVersion();
                var il = l.getLocation("");
                var result = String.format("Dependency found: %s in file:\n %s, at line: %d and position: %d",
                        String.format("%s:%s:%s", g, a, v), il.getSource().getLocation(), il.getLineNumber(),
                        il.getColumnNumber());
                results.add(new Result("", "maven", result));
            });
        } else {
            MavenGav mvnGav = getMavenGav(query.keyValues().get("gavs"));
            // Extract dependency search criteria
            String groupId = mvnGav.groupId();
            String artifactId = mvnGav.artifactId();
            String version = mvnGav.version();

            Optional<InputLocation> loc = findDependencyLocation(pomPath.toString(), groupId, artifactId, version);
            if (loc.isPresent()) {
                InputLocation il = loc.get();
                // The il.getSource().getModelId() returns the ID of the artifact within the model BUT not the artifact that we are looking for !
                var result = String.format("Dependency: %s found in file:\n %s\nat line: %d and position: %d",
                        formatGav(groupId, artifactId, version), il.getSource().getLocation(), il.getLineNumber(),
                        il.getColumnNumber());
                results.add(new Result("", "maven", result));
            }
        }

        return results;
    }

    private List<Dependency> findDependencies(String pomPath) {
        modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult result = buildModel(pomPath);

        List<Dependency> dependencies = searchDependencies(result.getEffectiveModel());

        // If not found with effective model, try with raw model to BOM's case
        /*
         * if (dependencies.isEmpty()) {
         * dependencies = searchDependencies(result.getRawModel());
         * }
         */

        return dependencies;
    }

    private List<Dependency> searchDependencies(Model model) {
        List<Dependency> dependencies = new ArrayList<>();
        if (model.getDependencies() != null) {
            dependencies.addAll(model.getDependencies());
        }
        /*
         * if (model.getDependencyManagement() != null) {
         * dependencies.addAll(model.getDependencyManagement().getDependencies());
         * }
         */
        return dependencies;
    }

    public Optional<InputLocation> findDependencyLocation(String pomPath, String groupId, String artifactId,
            String version) {
        modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult result = buildModel(pomPath);

        // First try with effective model (current behavior)
        Optional<InputLocation> location = searchDependency(result.getEffectiveModel(), pomPath, groupId, artifactId,
                version, true);

        // If not found with effective model, try with raw model to BOM's case
        if (!location.isPresent()) {
            location = searchDependency(result.getRawModel(), pomPath, groupId, artifactId, version, false);
        }

        return location;
    }

    public Optional<InputLocation> searchDependency(Model model, String pomPath, String groupId, String artifactId,
            String version, boolean isEffectiveModel) {

        if (model.getDependencies() != null) {
            //printDependencies(model.getDependencies());
            Optional<Dependency> dep = model.getDependencies().stream()
                    .filter(d -> matchesGav(d, groupId, artifactId, version, model, isEffectiveModel)).findFirst();

            if (dep.isPresent()) {
                // Found it!
                logger.debugf("Dep groupId location: %s", dep.get().getLocation("groupId"));
                logger.debugf("Dep artifactId location: %s", dep.get().getLocation("artifactId"));
                logger.debugf("Dep version location: %s", dep.get().getLocation("version"));
                logger.debugf("Dep location: %s", dep.get().getLocation(""));
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        if (model.getDependencyManagement() != null) {
            Optional<Dependency> dep = model.getDependencyManagement().getDependencies().stream()
                    .filter(d -> matchesGav(d, groupId, artifactId, version, model, isEffectiveModel)).findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        Parent p = model.getParent();
        if (p != null) {
            // If the GAV has not been found within the parent tag, then we will search about it within the parent pom: dependencies, dependencyManagement
            String parentRelativePath = p.getRelativePath();
            if (parentRelativePath != "") {
                String parentPomPath = Paths.get(new File(pomPath).getParent(), parentRelativePath).toString();
                return searchDependency(buildModel(parentPomPath).getEffectiveModel(), parentPomPath, groupId,
                        artifactId, version, true);
            } else {
                // GAV is defined part of the pom parent section
                if (matchesGav(p, groupId, artifactId, version, model, isEffectiveModel)) {
                    return Optional.ofNullable(p.getLocation(""));
                } else {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Extract the gav
     *
     * @param gavs The string of the gav definition: groupId:artifactId:(version)
     * @return the MavenGav
     */
    private MavenGav getMavenGav(String gavs) {
        String[] parts = gavs.split(":");

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid format for 'gavs'. 'groupId:artifactId[:version]' expected. Received: " + gavs);
        }

        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts.length > 2 ? parts[2] : "";

        return new MavenGav(groupId, artifactId, version);
    }

    /**
     * Matches a dependency GAV against the search criteria, handling both effective and raw models
     */
    private boolean matchesGav(Dependency dependency, String groupId, String artifactId, String version, Model model,
            boolean isEffectiveModel) {
        return matchesArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), groupId,
                artifactId, version, model, isEffectiveModel);
    }

    /**
     * Matches a parent GAV against the search criteria, handling both effective and raw models
     */
    private boolean matchesGav(Parent parent, String groupId, String artifactId, String version, Model model,
            boolean isEffectiveModel) {
        return matchesArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), groupId, artifactId,
                version, model, isEffectiveModel);
    }

    /**
     * Common logic for matching artifacts (dependencies or parents) against search criteria
     */
    private boolean matchesArtifact(String artifactGroupId, String artifactArtifactId, String artifactVersion,
            String searchGroupId, String searchArtifactId, String searchVersion, Model model,
            boolean isEffectiveModel) {
        String resolvedGroupId = artifactGroupId;
        String resolvedArtifactId = artifactArtifactId;
        String resolvedVersion = artifactVersion;

        // For raw model, resolve properties to match against the search criteria
        if (!isEffectiveModel) {
            resolvedGroupId = resolveProperty(resolvedGroupId, model);
            resolvedArtifactId = resolveProperty(resolvedArtifactId, model);
            resolvedVersion = resolveProperty(resolvedVersion, model);
        }

        // Check if groupId OR artifactId match
        if (!searchGroupId.equals(resolvedGroupId) || !searchArtifactId.equals(resolvedArtifactId)) {
            return false;
        }

        // Check version if specified
        boolean hasSearchVersion = (searchVersion != null && !searchVersion.isEmpty());
        if (!hasSearchVersion) {
            return true; // Match without version check
        } else {
            return searchVersion.equals(resolvedVersion);
        }
    }

    private ModelBuildingResult buildModel(String pomPath) {
        RepositoryModelResolver repositoryModelResolver = new RepositoryModelResolver();
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(new File(pomPath));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setSystemProperties(System.getProperties());
        req.setLocationTracking(true);
        req.setModelResolver(repositoryModelResolver);

        ModelBuildingResult result = null;
        try {
            return modelBuilder.build(req);
        } catch (Exception e) {
            logger.error("Could not build effective model: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Resolves property placeholders in a given value using the model's properties
     */
    private String resolveProperty(String value, Model model) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        String resolved = value;
        Properties properties = model.getProperties();

        // Simple property resolution - replace ${property.name} with actual value
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            String propertyName = (String) property.getKey();
            String propertyValue = (String) property.getValue();
            String placeholder = "${" + propertyName + "}";

            if (resolved.contains(placeholder)) {
                resolved = resolved.replace(placeholder, propertyValue);
            }
        }

        return resolved;
    }

    public String formatGav(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null) {
            throw new IllegalArgumentException("GroupId and ArtifactId cannot be null.");
        }

        List<String> parts = new ArrayList<>();
        parts.add(groupId);
        parts.add(artifactId);

        if (version != null && !version.isEmpty()) {
            parts.add(version);
        }

        return String.join(":", parts);
    }

}