package dev.snowdrop.mapper.java.pkg;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.JavaPackageDTO;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeMappingConfig;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Mapper for JavaPackageDTO-specific queries.
 * This mapper is optimized for Java package analysis and creates RecipeDTO objects
 * specifically tailored for package-level searches in Java source code.
 */
public class JavaPackageMapper implements QueryMapper<JavaPackageDTO> {

	private static final Logger logger = Logger.getLogger(JavaPackageMapper.class);

	private static final Map<String, RecipeMappingConfig> MAPPINGS = Map.of("JAVA.PACKAGE",
			new RecipeMappingConfig("", null, null));

	@Override
	public JavaPackageDTO map(Query query) {
		logger.debugf("Mapping query %s.%s for FileContentDTO", query.fileType(), query.symbol());
		return null;
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.JavaPackageDTO";
	}
}