package openrewrite.recipe.spring;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SpringBootScanReport extends DataTable<SpringBootScanReport.Row> {

    public SpringBootScanReport(@Nullable Recipe recipe) {
        super(recipe, "Spring Boot scanning report", "Record occurences of the Spring Boot annotations, etc. discovered");
    }

    @Value
    public static class Row {
        @Column(displayName = "Name",
            description = "Fully qualified name of the symbol.")
        String name;

        @Column(displayName = "Position",
            description = "Position. TODO")
        String position;
    }
}
