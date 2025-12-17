package dev.snowdrop.mtool.openrewrite.maven.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class DependencyReport extends DataTable<DependencyReport.Row> {
	public DependencyReport(Recipe recipe) {
		super(recipe, "Matching report",
				"Records file(s) where a matching condition succeeded to find a resource using a pattern.");
	}

	@Value
	public static class Row {
		@Column(displayName = "Match ID", description = "ID of the matching tool used to reconcile the information.")
		String matchId;

		@Column(displayName = "File's type", description = "Type of the file where we look for.")
		Type type;

		@Column(displayName = "Symbol searched", description = "Symbol about what we search about: dependency.")
		Symbol symbol;

		@Column(displayName = "A pattern", description = "The pattern, expressed as the concatenation of the fields searched.")
		String pattern;

		@Column(displayName = "Source file path", description = "Path of the source file where a match found")
		String sourceFilePath;
	}

	public enum Type {
		JAVA, POM, XML, JSON, YAML, PROPERTIES
	}

	public enum Symbol {
		DEPENDENCY
	}
}
