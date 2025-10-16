package dev.snowdrop.model;

import java.util.Map;

public record RecipeMappingConfig(
    String recipeFqn,
    Map<String, String> parameters,
    Map<String, String> additionalParameters
) {}