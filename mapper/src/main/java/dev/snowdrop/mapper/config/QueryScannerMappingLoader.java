package dev.snowdrop.mapper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for loading and parsing the query scanner mapping configuration.
 * Uses Jackson YAML parser to read the configuration file.
 */
public class QueryScannerMappingLoader {

	private static final Logger logger = Logger.getLogger(QueryScannerMappingLoader.class);
	private static final String CONFIG_FILE_NAME = "query-scanner-mapping.yml";
	private static QueryScannerMapping cachedMapping;

	private QueryScannerMappingLoader() {
	}

	/**
	 * Loads the query scanner mapping configuration from the YAML file.
	 * Uses caching to avoid multiple file reads.
	 *
	 * @return the parsed QueryScannerMapping configuration
	 * @throws RuntimeException if the configuration cannot be loaded
	 */
	public static QueryScannerMapping loadMapping() {
		if (cachedMapping == null) {
			cachedMapping = loadMappingFromFile();
		}
		return cachedMapping;
	}

	/**
	 * Forces a reload of the configuration from file, bypassing cache.
	 *
	 * @return the freshly loaded QueryScannerMapping configuration
	 * @throws RuntimeException if the configuration cannot be loaded
	 */
	public static QueryScannerMapping reloadMapping() {
		cachedMapping = loadMappingFromFile();
		return cachedMapping;
	}

	private static QueryScannerMapping loadMappingFromFile() {
		try {
			logger.infof("Loading query scanner mapping from %s", CONFIG_FILE_NAME);

			InputStream inputStream = QueryScannerMappingLoader.class.getClassLoader()
					.getResourceAsStream(CONFIG_FILE_NAME);

			if (inputStream == null) {
				throw new IllegalArgumentException("Configuration file not found: " + CONFIG_FILE_NAME);
			}

			ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
			QueryScannerMapping mapping = yamlMapper.readValue(inputStream, QueryScannerMapping.class);

			logger.infof("Successfully loaded query scanner mapping configuration");
			return mapping;

		} catch (IOException e) {
			String errorMsg = "Failed to load query scanner mapping configuration: " + e.getMessage();
			logger.errorf(errorMsg);
			throw new RuntimeException(errorMsg, e);
		}
	}

	/**
	 * Convenience method to get scanner configuration for a query.
	 *
	 * @param type   the query type (e.g., "java", "pom")
	 * @param symbol the query symbol (e.g., "annotation", "dependency")
	 * @return the scanner configuration
	 */
	public static ScannerConfig getScannerConfig(String type, String symbol) {
		return loadMapping().getScannerConfig(type, symbol);
	}
}