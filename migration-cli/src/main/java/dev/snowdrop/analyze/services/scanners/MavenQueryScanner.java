package dev.snowdrop.analyze.services.scanners;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.mapper.DynamicDTOMapper;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.model.MavenDependencyDTO;
import dev.snowdrop.model.Query;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.*;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Scanner implementation for Maven POM dependency queries.
 * Handles queries like pom.dependency.
 */
public class MavenQueryScanner implements QueryScanner {
	private static final Logger logger = Logger.getLogger(MavenQueryScanner.class);
	private ModelBuilder modelBuilder = null;
	private final QueryScannerMappingLoader queryScannerMappingLoader;

	public MavenQueryScanner() {
		this.queryScannerMappingLoader = new QueryScannerMappingLoader();
	}

	@Override
	public List<Match> executeQueries(Config config, Set<Query> queries) {
		logger.infof("Maven scanner executing %d queries", queries.size());

		List<Match> allResults = new ArrayList<>();

		for (Query query : queries) {
			// Get scanner configuration for this query
			ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());

			// Validate that this query should be handled by Maven scanner
			if (!"maven".equals(scannerConfig.getScanner())) {
				logger.warnf("Query %s.%s is configured for scanner '%s', not 'maven'. Skipping.", query.fileType(),
						query.symbol(), scannerConfig.getScanner());
				continue;
			}

			// Log the DTO class that should be used for this query
			String dtoClassName = scannerConfig.getDto();
			logger.debugf("Query %s.%s will use DTO: %s", query.fileType(), query.symbol(), dtoClassName);

			MavenDependencyDTO dto = DynamicDTOMapper.mapToDTO(query, dtoClassName);

			List<Match> queryResults = executeSingleQuery(config, scannerConfig.getScanner(), query, dto);
			allResults.addAll(queryResults);

			logger.debugf("Found %d matches for query %s.%s (DTO: %s)", queryResults.size(), query.fileType(),
					query.symbol(), dtoClassName);
		}

		logger.infof("Maven scanner completed. Total matches found: %d", allResults.size());
		return allResults;
	}

	@Override
	public String getScannerType() {
		return "maven";
	}

	@Override
	public boolean supports(Query query) {
		// Check the configuration to see if this query should use the Maven scanner
		ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
		return "maven".equals(scannerConfig.getScanner());
	}

	private List<Match> executeSingleQuery(Config config, String scannerType, Query query, MavenDependencyDTO dto) {
		List<Match> results = new ArrayList<>();

		logger.infof("Executing Maven dependency query: %s", query);

		// Extract dependency search criteria from DTO
		String groupId = dto != null ? dto.groupId() : query.keyValues().get("groupId");
		String artifactId = dto != null ? dto.artifactId() : query.keyValues().get("artifactId");
		String version = dto != null ? dto.version() : query.keyValues().get("version");

		// Find and analyze pom.xml file
		Path pomPath = Paths.get(config.appPath(), "pom.xml");

		Optional<InputLocation> loc = findDependencyLocation(pomPath.toString(), groupId, artifactId, version);
		if (loc.isPresent()) {
			InputLocation il = loc.get();
			// The il.getSource().getModelId() returns the ID of the artifact within the model BUT not the artifact that we are looking for !
			var result = String.format("Dependency: %s found in file:\n %s\nat line: %d and position: %d",
					formatGav(groupId, artifactId, version), il.getSource().getLocation(), il.getLineNumber(),
					il.getColumnNumber());
			results.add(new Match("", "maven", result));
		}

		return results;
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

		//model.getBuild().getPlugins().stream().forEach(p -> {System.out.printf("Plugin gav: %s:%s:%s\n", p.getGroupId(), p.getArtifactId(), p.getVersion());});

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
			//logger.info("========= Dependencies of DependencyManagement ==========");
			//printDependencies(model.getDependencyManagement().getDependencies());
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