package dev.snowdrop.openrewrite.recipe.query;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryValidator {

    private static final Map<String, Set<String>> VALID_KEYS_PER_TYPE = Map.of(
        "DEPENDENCY", Set.of("groupId", "artifactId", "version"),
        "PROPERTY", Set.of("name", "value"),
        "CLASS", Set.of("name", "modifiers"),
        "METHOD", Set.of("name", "modifiers")
    );

    public static void validate(String elementType, Condition condition) {
        Set<String> validKeys = VALID_KEYS_PER_TYPE.get(elementType.toUpperCase());
        if (validKeys == null) {
            throw new IllegalArgumentException("Unknown element type for validation: " + elementType);
        }

        Set<String> usedKeys = collectKeys(condition);
        Set<String> invalidKeys = usedKeys.stream()
            .filter(key -> !validKeys.contains(key))
            .collect(Collectors.toSet());

        if (!invalidKeys.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Invalid parameter(s) for element type '%s': %s. Allowed parameters are: %s",
                elementType, invalidKeys, validKeys
            ));
        }
    }

    private static Set<String> collectKeys(Condition condition) {
        Set<String> keys = new HashSet<>();
        if (condition instanceof SimpleCondition sc) {
            keys.add(sc.key());
        } else if (condition instanceof AndCondition ac) {
            keys.addAll(collectKeys(ac.left()));
            keys.addAll(collectKeys(ac.right()));
        } else if (condition instanceof OrCondition oc) {
            keys.addAll(collectKeys(oc.left()));
            keys.addAll(collectKeys(oc.right()));
        }
        return keys;
    }
}