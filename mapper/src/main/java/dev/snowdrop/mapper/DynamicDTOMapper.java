package dev.snowdrop.mapper;

import dev.snowdrop.mapper.java.annotation.JavaAnnotationMapper;
import dev.snowdrop.mapper.java.clazz.JavaClassMapper;
import dev.snowdrop.mapper.maven.dependency.MavenDependencyMapper;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic mapper that creates appropriate DTO objects based on the configured DTO class.
 * Each DTO type now has its own specialized mapper implementation.
 */
public class DynamicDTOMapper {

	private static final Logger logger = Logger.getLogger(DynamicDTOMapper.class);

	// Registry of mappers for each DTO type
	private static final Map<String, QueryMapper> MAPPER_REGISTRY = new HashMap<>();

	static {
		// Initialize mapper registry with all available mappers
		registerMapper(new JavaAnnotationMapper());
		registerMapper(new JavaClassMapper());
		registerMapper(new MavenDependencyMapper());
	}

	/**
	 * Registers a mapper in the registry.
	 */
	private static void registerMapper(QueryMapper mapper) {
		MAPPER_REGISTRY.put(mapper.getSupportedDtoClass(), mapper);
	}

	/**
	 * Maps a Query to the appropriate DTO based on the configured DTO class name.
	 * The configured DTO class determines which specialized mapper is used and
	 * what type of DTO is returned.
	 *
	 * @param query The query to map
	 * @param dtoClassName The fully qualified class name of the configured DTO
	 * @param <T> The type of DTO to return
	 * @return DTO object of the type specified by the mapper for dtoClassName
	 */
	@SuppressWarnings("unchecked")
	public static <T> T mapToDTO(Query query, String dtoClassName) {
		logger.debugf("Mapping query %s.%s to DTO using configured class: %s", query.fileType(), query.symbol(),
				dtoClassName);

		// Look up the appropriate mapper for this DTO type
		QueryMapper mapper = MAPPER_REGISTRY.get(dtoClassName);

		if (mapper != null) {
			logger.debugf("Using %s for DTO type: %s", mapper.getClass().getSimpleName(), dtoClassName);
			return (T) mapper.map(query);
		} else {
			logger.warnf("No specific mapper found for DTO class: %s.", dtoClassName);
			return null;
		}
	}

	/**
	 * Gets all registered DTO types.
	 *
	 * @return Set of all registered DTO class names
	 */
	public static java.util.Set<String> getRegisteredDtoTypes() {
		return MAPPER_REGISTRY.keySet();
	}

}