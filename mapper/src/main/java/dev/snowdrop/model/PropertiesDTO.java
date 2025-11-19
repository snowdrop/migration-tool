package dev.snowdrop.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTO for properties file analysis results from file scanner.
 * Contains information about properties found in configuration files.
 */
public record PropertiesDTO(String filePath, String fileName, String fileType, Map<String, String> properties,
		Set<String> profiles, List<String> comments, Map<String, String> environmentVariables, boolean isYamlFormat,
		Map<String, Object> nestedProperties, Set<String> missingProperties, Map<String, String> defaultValues) {

	/**
	 * Creates a new PropertiesDTO with additional properties.
	 */
	public PropertiesDTO withAdditionalProperties(Map<String, String> additionalProperties) {
		Map<String, String> updatedProperties = new java.util.HashMap<>(properties);
		updatedProperties.putAll(additionalProperties);
		return new PropertiesDTO(filePath, fileName, fileType, updatedProperties, profiles, comments,
				environmentVariables, isYamlFormat, nestedProperties, missingProperties, defaultValues);
	}

	/**
	 * Creates a new PropertiesDTO with additional profiles.
	 */
	public PropertiesDTO withAdditionalProfiles(Set<String> additionalProfiles) {
		Set<String> updatedProfiles = new java.util.HashSet<>(profiles);
		updatedProfiles.addAll(additionalProfiles);
		return new PropertiesDTO(filePath, fileName, fileType, properties, updatedProfiles, comments,
				environmentVariables, isYamlFormat, nestedProperties, missingProperties, defaultValues);
	}

	/**
	 * Creates a new PropertiesDTO with additional environment variables.
	 */
	public PropertiesDTO withAdditionalEnvironmentVariables(Map<String, String> additionalEnvVars) {
		Map<String, String> updatedEnvVars = new java.util.HashMap<>(environmentVariables);
		updatedEnvVars.putAll(additionalEnvVars);
		return new PropertiesDTO(filePath, fileName, fileType, properties, profiles, comments, updatedEnvVars,
				isYamlFormat, nestedProperties, missingProperties, defaultValues);
	}

	/**
	 * Returns the value of a specific property.
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Returns the value of a property with a default fallback.
	 */
	public String getProperty(String key, String defaultValue) {
		return properties.getOrDefault(key, defaultValue);
	}

	/**
	 * Returns whether a specific property exists.
	 */
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	/**
	 * Returns all property keys.
	 */
	public Set<String> getPropertyKeys() {
		return properties.keySet();
	}

	/**
	 * Returns the number of properties.
	 */
	public int getPropertyCount() {
		return properties.size();
	}

	/**
	 * Returns whether the file uses YAML format.
	 */
	public boolean isYaml() {
		return isYamlFormat;
	}

	/**
	 * Returns whether the file is a standard properties file.
	 */
	public boolean isPropertiesFormat() {
		return !isYamlFormat;
	}

	/**
	 * Returns properties that match a specific prefix.
	 */
	public Map<String, String> getPropertiesWithPrefix(String prefix) {
		return properties.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
				.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}