package dev.snowdrop.mapper.java.annotation;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory mapper for JavaAnnotation-specific queries.
 * This factory routes to the appropriate scanner-specific mapper based on configuration.
 * Supports multiple scanners with their specific DTO types and mapping logic.
 */
public class JavaAnnotationMapper implements QueryMapper<Object> {
	private static final Logger logger = Logger.getLogger(JavaAnnotationMapper.class);

	// Registry of scanner-specific mappers
	private static final Map<String, QueryMapper<Object>> SCANNER_MAPPERS = new HashMap<>();

	private final QueryScannerMappingLoader queryScannerMappingLoader;

	static {
		// Initialize scanner-specific mappers
		SCANNER_MAPPERS.put("openrewrite", new OpenRewriteJavaAnnotationMapper());
		SCANNER_MAPPERS.put("jdtls", new JdtlsJavaAnnotationMapper());
	}

	public JavaAnnotationMapper() {
		queryScannerMappingLoader = new QueryScannerMappingLoader();
	}

	@Override
	public Object map(Query query) {
		logger.debugf("Mapping query %s.%s using factory pattern", query.fileType(), query.symbol());

		// Get scanner configuration for this query to determine scanner type
		ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
		String scannerType = scannerConfig.getScanner();

		logger.debugf("Using scanner type: %s for query %s.%s", scannerType, query.fileType(), query.symbol());

		// Factory pattern: delegate to appropriate scanner-specific mapper
		QueryMapper<Object> scannerMapper = SCANNER_MAPPERS.get(scannerType.toLowerCase());
		if (scannerMapper != null) {
			return scannerMapper.map(query);
		} else {
			logger.warnf("No mapper found for scanner type: %s. Available mappers: %s", scannerType,
					SCANNER_MAPPERS.keySet());
			throw new IllegalArgumentException("Unsupported scanner type: " + scannerType);
		}
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.JavaAnnotationDTO";
	}
}