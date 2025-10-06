package dev.snowdrop.transform.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.snowdrop.analyze.model.MigrationTask;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MigrationTasksExport(
    String title,
    String projectPath,
    String timestamp,
    Map<String, MigrationTask> migrationTasks
) {
}
