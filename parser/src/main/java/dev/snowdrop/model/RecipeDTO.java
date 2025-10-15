package dev.snowdrop.model;

import java.util.List;

public record RecipeDTO(
    String name,
    List<Parameter> parameters
) {}
