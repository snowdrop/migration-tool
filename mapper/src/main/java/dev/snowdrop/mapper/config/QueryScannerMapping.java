package dev.snowdrop.mapper.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Root configuration class for query scanner and DTO mapping.
 * Parses the query-scanner-mapping.yml file.
 */
public class QueryScannerMapping {

	@JsonProperty("java")
	private Map<String, ScannerConfig> javaQueries;

	@JsonProperty("pom")
	private Map<String, ScannerConfig> pomQueries;

	@JsonProperty("gradle")
	private Map<String, ScannerConfig> gradleQueries;

	@JsonProperty("file")
	private Map<String, ScannerConfig> fileQueries;

	@JsonProperty("default")
	private ScannerConfig defaultConfig;

	// Constructors
	public QueryScannerMapping() {
	}

	// Getters and setters
	public Map<String, ScannerConfig> getJavaQueries() {
		return javaQueries;
	}

	public void setJavaQueries(Map<String, ScannerConfig> javaQueries) {
		this.javaQueries = javaQueries;
	}

	public Map<String, ScannerConfig> getPomQueries() {
		return pomQueries;
	}

	public void setPomQueries(Map<String, ScannerConfig> pomQueries) {
		this.pomQueries = pomQueries;
	}

	public Map<String, ScannerConfig> getGradleQueries() {
		return gradleQueries;
	}

	public void setGradleQueries(Map<String, ScannerConfig> gradleQueries) {
		this.gradleQueries = gradleQueries;
	}

	public Map<String, ScannerConfig> getFileQueries() {
		return fileQueries;
	}

	public void setFileQueries(Map<String, ScannerConfig> fileQueries) {
		this.fileQueries = fileQueries;
	}

	public ScannerConfig getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(ScannerConfig defaultConfig) {
		this.defaultConfig = defaultConfig;
	}

	/**
	 * Retrieves scanner configuration for a given query type and symbol.
	 *
	 * @param type   the query type (e.g., "java", "pom", "gradle", "file")
	 * @param symbol the query symbol (e.g., "annotation", "dependency")
	 * @return the scanner configuration, or default if not found
	 */
	public ScannerConfig getScannerConfig(String type, String symbol) {
		Map<String, ScannerConfig> typeQueries = getQueriesByType(type);

		if (typeQueries != null && typeQueries.containsKey(symbol)) {
			return typeQueries.get(symbol);
		}

		return defaultConfig;
	}

	private Map<String, ScannerConfig> getQueriesByType(String type) {
		return switch (type.toLowerCase()) {
			case "java" -> javaQueries;
			case "pom" -> pomQueries;
			case "gradle" -> gradleQueries;
			case "file" -> fileQueries;
			default -> null;
		};
	}
}