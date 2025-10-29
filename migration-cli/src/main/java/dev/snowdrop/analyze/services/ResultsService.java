package dev.snowdrop.analyze.services;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.ColumnData;
import com.github.freva.asciitable.HorizontalAlign;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResultsService {

    public static void showCsvTable(Map<String, MigrationTask> results, String source, String target) {
        // Prepare data for the table
        List<String[]> tableData = new ArrayList<>();

        for (Map.Entry<String, MigrationTask> entry : results.entrySet()) {
            String ruleId = entry.getKey();
            MigrationTask aTask = entry.getValue();

            List<?> queryResults = new ArrayList<>();

            if (aTask.getLsResults() != null && !aTask.getLsResults().isEmpty()) {
                queryResults = aTask.getLsResults();
            }

            if (aTask.getRewriteResults() != null && !aTask.getRewriteResults().isEmpty()) {
                queryResults = aTask.getRewriteResults();
            }

            String hasQueryResults = queryResults.isEmpty() ? "No" : "Yes";
            String sourceToTarget = String.format("%s -> %s", source, target);

            if (queryResults.isEmpty()) {
                tableData.add(new String[] { ruleId, sourceToTarget, hasQueryResults, "No match found" });
            } else {
                // Process ALL results, not just the first one
                StringBuilder allResultsDetails = new StringBuilder();

                for (int i = 0; i < queryResults.size(); i++) {
                    Object result = queryResults.get(i);

                    if (result instanceof SymbolInformation symbolInfo) {
                        String symbolDetails = formatSymbolInformation(symbolInfo);
                        allResultsDetails.append(symbolDetails).append("\n").append(symbolInfo.getLocation().getUri());
                    } else if (result instanceof Rewrite rewrite) {
                        String rewriteDetails = formatRewriteImproved(rewrite);
                        allResultsDetails.append(rewriteDetails);
                    } else {
                        // Fallback for unknown types
                        allResultsDetails.append("Unknown result type: ").append(result.getClass().getSimpleName());
                    }

                    // Add separator between multiple results (except for the last one)
                    if (i < queryResults.size() - 1) {
                        allResultsDetails.append("\n--- rewrite ---\n");
                    }
                }

                tableData.add(new String[] { ruleId, sourceToTarget, hasQueryResults, allResultsDetails.toString() });
            }
        }

        List<ColumnData<String[]>> columns = Arrays.asList(
                new Column().header("Rule ID").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT)
                        .with(row -> row[0]),
                new Column().header("Source to Target").headerAlign(HorizontalAlign.LEFT)
                        .dataAlign(HorizontalAlign.LEFT).with(row -> row[1]),
                new Column().header("Found").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                        .with(row -> row[2]),
                new Column().header("Information Details").headerAlign(HorizontalAlign.LEFT)
                        .dataAlign(HorizontalAlign.LEFT).maxWidth(100).with(row -> row[3]));

        // Use System.out.println instead of logger to avoid log formatting
        System.out.println("\n=== Code Analysis Results (Improved Formatting) ===");
        System.out.println(AsciiTable.getTable(tableData, columns));
    }

    private static String formatRewriteImproved(Rewrite rewrite) {
        String name = rewrite.name();

        // Parse the name format: parentFolderName/csvFileName:line_number|pattern.symbol|type
        if (name.contains("/") && name.contains(":") && name.contains("|")) {
            try {
                String[] pathAndRest = name.split(":", 2);
                String path = pathAndRest[0];
                String[] lineAndDetails = pathAndRest[1].split("\\|", 3);
                String lineNumber = lineAndDetails[0];
                String patternSymbol = lineAndDetails.length > 1 ? lineAndDetails[1] : "N/A";
                String type = lineAndDetails.length > 2 ? lineAndDetails[2] : "N/A";

                return String.format("File: %s\nLine: %s\nPattern: %s\nType: %s\nMatch ID: %s", path, lineNumber,
                        patternSymbol, type, rewrite.matchId());
            } catch (Exception e) {
                // Fallback if parsing fails
                return String.format("Rewrite match: %s (ID: %s)", rewrite.name(), rewrite.matchId());
            }
        } else {
            // Fallback for unexpected format
            return String.format("Rewrite match: %s (ID: %s)", rewrite.name(), rewrite.matchId());
        }
    }

    private static String formatSymbolInformation(SymbolInformation si) {
        return String.format("Found %s at line %s, char: %s - %s", si.getName(),
                si.getLocation().getRange().getStart().getLine() + 1,
                si.getLocation().getRange().getStart().getCharacter(),
                si.getLocation().getRange().getEnd().getCharacter());
    }

}