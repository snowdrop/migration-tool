package dev.snowdrop.mapper.maven.dependency;

import dev.snowdrop.mapper.QueryMapper;
import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeMappingConfig;
import dev.snowdrop.reconciler.MatchingUtils;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.mapper.MapperUtils.orderedMap;

public class OpenRewriteMavenDependencyMapper implements QueryMapper<Object> {
	private static final Logger logger = Logger.getLogger(OpenRewriteMavenDependencyMapper.class);

	private static final Map<String, RecipeMappingConfig> MAPPINGS = Map.of("POM.DEPENDENCY",
			new RecipeMappingConfig("dev.snowdrop.openrewrite.maven.search.FindDependency",
					// 1 to 1 translation for GAV properties
					orderedMap("gavs", "gavs", "groupId", "groupId", "artifactId", "artifactId", "version", "version"),
					// No additional parameters
					Map.of()));

	@Override
	public Object map(Query query) {
		logger.debugf("Creating RecipeDTO for OpenRewrite scanner: %s.%s", query.fileType(), query.symbol());

		String mappingKey = (query.fileType() + "." + query.symbol()).toUpperCase();

		RecipeMappingConfig config = MAPPINGS.get(mappingKey);
		if (config == null) {
			throw new IllegalArgumentException("No recipe mapping found for: " + mappingKey);
		}

		// Get the Recipe Class FQN
		String recipeName = config.recipeFqn();

		// Translate and add parameters from the original query
		List<Parameter> parameters = new ArrayList<>();

		for (Map.Entry<String, String> mappingEntry : config.parameters().entrySet()) {
			String queryKey = mappingEntry.getKey(); // e.g., "name"
			String translatedKey = mappingEntry.getValue(); // e.g., "pattern"

			// Check if the incoming query actually has this key
			if (query.keyValues().containsKey(queryKey)) {
				String value = query.keyValues().get(queryKey);
				parameters.add(new Parameter(translatedKey, value));
			}
		}

		parameters.add(new Parameter("matchId", MatchingUtils.generateUID().toString()));

		// Add any additional/default parameters from the config
		config.additionalParameters().forEach((key, value) -> parameters.add(new Parameter(key, value)));

		return new RecipeDTO(null, recipeName, parameters);
	}

	@Override
	public String getSupportedDtoClass() {
		return "";
	}
}
