package dev.snowdrop.mtool.scanner.openrewrite;

import java.util.List;

public class RecipeHolder {
	// The OpenRewrite API version (e.g. )
	private String ApiVersion = "specs.openrewrite.org/v1beta/recipe";

	// A fully qualified, unique name for the recipe holder (e.g. org.openrewrite.java.apache.httpclient5.UpgradeApacheHttpClient_5)
	private String name;

	// A human-readable name for this recipe (does not end with a period)
	private String displayName;

	// A human-readable description for this recipe (ends with a period)
	private String description;

	// The list of recipes held by this "Recipe"
	private List<RecipeDefinition> recipesList;

	public String getApiVersion() {
		return ApiVersion;
	}

	public void setApiVersion(String apiVersion) {
		ApiVersion = apiVersion;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<RecipeDefinition> getRecipesList() {
		return recipesList;
	}

	public void setRecipesList(List<RecipeDefinition> recipesList) {
		this.recipesList = recipesList;
	}
}
