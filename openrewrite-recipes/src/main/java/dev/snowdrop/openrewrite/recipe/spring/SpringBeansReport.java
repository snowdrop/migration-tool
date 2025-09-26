package dev.snowdrop.openrewrite.recipe.spring;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class SpringBeansReport extends DataTable<SpringBeansReport.Row> {

    public SpringBeansReport(Recipe recipe) {
        super(recipe, "Spring bean definitions",
            "Classes defined with a form of a Spring `@Bean` stereotype");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
            description = "The path to the source file containing the component definition.")
        String sourcePath;

        @Column(displayName = "Component name",
            description = "The name of the component.")
        String name;
    }
}
