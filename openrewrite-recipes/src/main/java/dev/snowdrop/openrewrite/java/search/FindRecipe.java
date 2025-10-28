package dev.snowdrop.openrewrite.java.search;

import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.AnnotationMatcher;

public abstract class FindRecipe extends Recipe {

    /**
     * A symbol pattern, expressed as a method pattern to search about: annotation, field, method, etc.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Symbol pattern",
        description = "A symbol pattern, expressed as a method pattern to search about: annotation, field, method, etc.",
        example = "@java.lang.SuppressWarnings(\"deprecation\")")
    public String pattern;

    /**
     * ID of the matching tool needed to reconcile the records where a match took place
     */
    @Option(displayName = "Match id",
        description = "ID of the matching tool needed to reconcile the records where a match took place",
        required = true)
    public String matchId;
}
