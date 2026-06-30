package dev.snowdrop.mtool.model.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.snowdrop.mtool.model.analyze.Result;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ScanResultsExport(String title, String projectPath, String timestamp,
        Map<String, List<Result>> results) {
}