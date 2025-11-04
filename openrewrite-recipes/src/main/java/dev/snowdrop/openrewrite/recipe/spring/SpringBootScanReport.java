package dev.snowdrop.openrewrite.recipe.spring;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SpringBootScanReport extends DataTable<SpringBootScanReport.Row> {

	public SpringBootScanReport(@Nullable Recipe recipe) {
		super(recipe, "Spring Boot scanning report",
				"Record occurrences of the Spring Boot annotations, etc. discovered");
	}

	public static class Row {
		@Column(displayName = "Name", description = "Fully qualified name of the symbol.")
		String name;

		@Column(displayName = "Position", description = "Position. TODO")
		String position;

		public Row(String name, String position) {
			this.name = name;
			this.position = position;
		}

		public String getName() {
			return name;
		}

		public String getPosition() {
			return position;
		}
	}
}
