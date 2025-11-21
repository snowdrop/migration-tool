package dev.snowdrop.mapper.properties;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.PropertiesDTO;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.reconciler.MatchingUtils;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for PropertiesDTO-specific queries.
 * This mapper is optimized for properties file analysis and creates RecipeDTO objects
 * specifically tailored for searching configuration properties.
 */
public class PropertiesMapper implements QueryMapper<PropertiesDTO> {

	private static final Logger logger = Logger.getLogger(PropertiesMapper.class);

	@Override
	public PropertiesDTO map(Query query) {
		logger.debugf("Mapping query %s.%s for PropertiesDTO", query.fileType(), query.symbol());
		return null;
	}

	@Override
	public String getSupportedDtoClass() {
		return "dev.snowdrop.model.PropertiesDTO";
	}
}