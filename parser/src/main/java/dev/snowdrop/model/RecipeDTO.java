package dev.snowdrop.model;

import java.util.List;
import java.util.Optional;

public record RecipeDTO(
    Optional<String> id,
    String name,
    List<Parameter> parameters
) {}
