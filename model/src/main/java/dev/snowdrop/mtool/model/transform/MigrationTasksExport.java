package dev.snowdrop.mtool.model.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.snowdrop.mtool.model.analyze.MigrationTask;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MigrationTasksExport(String title, String projectPath, String timestamp,
		Map<String, MigrationTask> migrationTasks) {
}
