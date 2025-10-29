package dev.snowdrop.transform.model;

import java.util.List;

public record CompositeRecipe(String type, String name, String displayName, String description,
        List<Object> recipeList) {

    public CompositeRecipe(String name, String displayName, String description, List<Object> recipeList) {
        this("specs.openrewrite.org/v1beta/recipe", name, displayName, description, recipeList);
    }
}