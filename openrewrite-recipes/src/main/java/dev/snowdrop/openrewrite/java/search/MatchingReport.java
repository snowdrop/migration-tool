package dev.snowdrop.openrewrite.java.search;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class MatchingReport extends DataTable<MatchingReport.Row> {
    public MatchingReport(Recipe recipe) {
        super(recipe,
            "Matching report",
            "Records file(s) where a matching condition succeeded to find a resource.");
    }

    @Value
    public static class Row {
        @Column(displayName = "File's type",
            description = "Type of the file where we look for.")
        Type type;

        @Column(displayName = "Source file path",
            description = "Path of the source file where a match found")
        String sourceFilePath;

        @Column(displayName = "FQName of the Class",
            description = "FQName of the Class containing the symbol we search.")
        String className;

        @Column(displayName = "Symbol searched",
            description = "Symbol about what we search about: annotation, import, method, filed, etc.")
        String symbol;

        @Column(displayName = "Match ID",
            description = "ID of the matching tool used to reconcile the information.")
        String matchId;
    }

    public enum Type {
        JAVA,
        POM,
        XML,
        JSON,
        YAML,
        PROPERTIES
    }
}
