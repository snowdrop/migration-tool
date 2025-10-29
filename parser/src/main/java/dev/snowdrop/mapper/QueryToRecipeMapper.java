package dev.snowdrop.mapper;

import dev.snowdrop.model.Parameter;
import dev.snowdrop.model.Query;
import dev.snowdrop.model.RecipeDTO;
import dev.snowdrop.model.RecipeMappingConfig;
import dev.snowdrop.reconciler.MatchingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryToRecipeMapper {

    private static final Map<String, RecipeMappingConfig> MAPPINGS = Map.of("JAVA.ANNOTATION",
            new RecipeMappingConfig("dev.snowdrop.openrewrite.java.search.FindAnnotations", Map.of("name", "pattern" // Translate
                                                                                                                     // query
                                                                                                                     // 'name'
                                                                                                                     // to
                                                                                                                     // recipe
                                                                                                                     // 'pattern'
            ),
                    // Additional parameters
                    Map.of("matchOnMetaAnnotations", "false" // key and value
                    )),

            "POM.DEPENDENCY",
            new RecipeMappingConfig("org.openrewrite.maven.search.FindDependency", Map.of("groupId", "groupId", // 1 to
                                                                                                                // 1
                                                                                                                // translation
                    "artifactId", "artifactId", // 1 to 1 translation
                    "version", "version" // 1 to 1 translation
            ),
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
        for (Map.Entry<String, String> entry : query.keyValues().entrySet()) {
            String key = entry.getKey();
            String translatedKey = config.parameters().get(key);

            if (translatedKey != null) {
                parameters.add(new Parameter(translatedKey, entry.getValue()));
            }
            // TODO: You could add a warning here for untranslatable keys
        }

        parameters.add(new Parameter("matchId", MatchingUtils.generateUID().toString()));

        // Add any additional/default parameters from the config
        config.additionalParameters().forEach((key, value) -> parameters.add(new Parameter(key, value)));

        return new RecipeDTO(null, recipeName, parameters);
    }
}
