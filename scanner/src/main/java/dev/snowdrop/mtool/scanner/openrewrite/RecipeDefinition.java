package dev.snowdrop.mtool.scanner.openrewrite;

import java.util.Map;

public class RecipeDefinition {
	// A fully qualified, unique name for the recipe holder (e.g. org.openrewrite.java.search.FindAnnotations)
	private String fqName;
	private Map<String, String> fieldMappings;

	public RecipeDefinition withFullyQualifyRecipeName(String fqName) {
		this.fqName = fqName;
		return this;
	}

	public RecipeDefinition withFieldMappings(Map<String, String> fieldMappings) {
		this.fieldMappings = fieldMappings;
		return this;
	}

	public String getFqName() {
		return fqName;
	}

	public void setFqName(String fqName) {
		this.fqName = fqName;
	}

	public Map<String, String> getFieldMappings() {
		return fieldMappings;
	}

	public void setFieldMappings(Map<String, String> fieldMappings) {
		this.fieldMappings = fieldMappings;
	}
}
