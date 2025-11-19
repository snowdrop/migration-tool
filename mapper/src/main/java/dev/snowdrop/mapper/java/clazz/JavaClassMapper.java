package dev.snowdrop.mapper.java.clazz;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.mapper.java.annotation.JdtlsJavaAnnotationMapper;
import dev.snowdrop.mapper.java.annotation.OpenRewriteJavaAnnotationMapper;
import dev.snowdrop.model.*;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for JavaClassDTO-specific queries.
 * This mapper is optimized for Java package analysis and creates JavaClassDTO objects
 */
public class JavaClassMapper implements QueryMapper {

	private static final Logger logger = Logger.getLogger(JavaClassMapper.class);

	// Registry of scanner-specific mappers
	private static final Map<String, QueryMapper<Object>> SCANNER_MAPPERS = new HashMap<>();

	static {
		// Initialize scanner-specific mappers
		SCANNER_MAPPERS.put("jdtls", new JdtlsJavaClassMapper());
	}

	@Override
	public Object map(Query query) {
		logger.debugf("Mapping query %s.%s using factory pattern", query.fileType(), query.symbol());

		// Get scanner configuration for this query to determine scanner type
		ScannerConfig scannerConfig = QueryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
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
		return "dev.snowdrop.model.JavaClassDTO";
	}
}