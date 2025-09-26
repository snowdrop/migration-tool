package dev.snowdrop.openrewrite.recipe.spring;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class SpringBeansReport extends DataTable<SpringBeansReport.Row> {

    public SpringBeansReport(Recipe recipe) {
        super(recipe, "Spring bean definitions",
            "Classes defined with a form of a Spring `@Bean` stereotype");
    }

    public static class Row {
        @Column(displayName = "Source path",
            description = "The path to the source file containing the component definition.")
        String sourcePath;

        @Column(displayName = "Component name",
            description = "The name of the component.")
        String name;

        public Row(String sourcePath, String name) {
            this.sourcePath = sourcePath;
            this.name = name;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getName() {
            return name;
        }
    }
}
