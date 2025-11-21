package dev.snowdrop.model;

import java.util.List;
import java.util.UUID;

public record RecipeDTO(UUID id, String name, List<Parameter> parameters) {
	public RecipeDTO withId(UUID newId) {
		return new RecipeDTO(newId, this.name, this.parameters);
	}
}
