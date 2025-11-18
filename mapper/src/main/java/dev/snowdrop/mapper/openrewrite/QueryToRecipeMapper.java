package dev.snowdrop.mapper.openrewrite;

import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeMappingConfig;
import dev.snowdrop.reconciler.MatchingUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryToRecipeMapper {

	private static final Map<String, RecipeMappingConfig> MAPPINGS = Map.of("JAVA.ANNOTATION",
			new RecipeMappingConfig(
					"dev.snowdrop.openrewrite.java.search.FindAnnotations", orderedMap("name", "pattern"),
					// Additional parameters
					orderedMap("matchOnMetaAnnotations", "false")),

			"POM.DEPENDENCY", new RecipeMappingConfig("dev.snowdrop.openrewrite.maven.search.FindDependency",
					// 1 to 1 translation for GAV properties
					orderedMap("gavs", "gavs", "groupId", "groupId", "artifactId", "artifactId", "version", "version"),
					// No additional parameters
					Map.of()));

	public static RecipeDTO map(Query query) {
		String mappingKey = (query.fileType() + "." + query.symbol()).toUpperCase();

		RecipeMappingConfig config = MAPPINGS.get(mappingKey);
		if (config == null) {
			throw new IllegalArgumentException("No recipe mapping found for: " + mappingKey);
		}

		// Get the Recipe Class FQN
		String recipeName = config.recipeFqn();

		// Translate and add parameters from the original query
		List<Parameter> parameters = new ArrayList<>();
		/*        for (Map.Entry<String, String> entry : query.keyValues().entrySet()) {
		    String key = entry.getKey();
		    String translatedKey = config.parameters().get(key);

		    if (translatedKey != null) {
		        parameters.add(new Parameter(translatedKey, entry.getValue()));
		    }
		    // TODO: You could add a warning here for untranslatable keys
		}*/
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

	/**
	 * Helper to create a Map that preserves insertion order.
	 * (Java's Map.of() does not guarantee order).
	 */
	private static Map<String, String> orderedMap(String... keyValues) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i + 1 < keyValues.length) {
				map.put(keyValues[i], keyValues[i + 1]);
			}
		}
		return map;
	}
}
