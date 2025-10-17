package dev.snowdrop.model;

import java.util.List;
import java.util.Optional;

public record RecipeDTO(
    String id,
    String name,
    List<Parameter> parameters
) {
    public RecipeDTO withId(String newId) {
        return new RecipeDTO(newId, this.name, this.parameters);
    }
}
