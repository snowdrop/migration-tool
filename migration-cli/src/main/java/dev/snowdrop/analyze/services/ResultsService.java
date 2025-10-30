package dev.snowdrop.analyze.services;

import com.github.freva.asciitable.*;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultsService {

    final static String RESET = "\u001B[m";
    final static String GREEN = "\u001B[32m";
    final static String YELLOW = "\u001B[33m";

    // TODO: Move the RULE_REPO to a quarkus property
    final static String RULE_REPO_URL = "https://github.com/snowdrop/migration-tool/blob/main/cookbook/rules/quarkus/%s.yaml";

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

        System.out.println("==== Data ====");
        tableData.forEach(row -> System.out.println(Arrays.toString(row)));

        System.out.println("\n=== Code Analysis Results ===");
        String asciiTable = AsciiTable.builder().styler(customizeStyle())
                .data(tableData,
                        Arrays.asList(
                                new Column().header("Rule ID").headerAlign(HorizontalAlign.LEFT)
                                        .dataAlign(HorizontalAlign.LEFT).with(r -> r[0]),
                                new Column().header("Source to Target").headerAlign(HorizontalAlign.LEFT)
                                        .dataAlign(HorizontalAlign.LEFT).with(r -> r[1]),
                                new Column().header("Match").headerAlign(HorizontalAlign.CENTER)
                                        .dataAlign(HorizontalAlign.CENTER).with(r -> r[2]),
                                new Column().header("Information Details").headerAlign(HorizontalAlign.LEFT)
                                        .maxWidth(120).dataAlign(HorizontalAlign.LEFT).with(r -> r[3])))
                .asString();
        System.out.println(asciiTable);
    }

    /**
     * Helper function to create an OSC 8 terminal hyperlink.
     * <p>
     * ESC ]8;\nLINK ST TEXT ESC ]8;\nST
     * <p>
     * ESC is the ESCAPE character ST is the String terminator
     * <p>
     * Example of commands printf '\u001B]8;;https://google.com\u001B\\Click Me\u001B]8;;\u001B\\ \n'
     */
    public static String createLink(String url, String text) {
        String ESC_CHAR = "\u001B"; // ESCAPE character
        String ST = "\u001B\\"; // String Terminator
        String HYPERLINK_CMD = "]8;;"; // OS command to crate a hyperlink
        String OSC8_START = ESC_CHAR + HYPERLINK_CMD + url + ST;
        String OSC8_END = ESC_CHAR + HYPERLINK_CMD + ST;
        return OSC8_START + text + OSC8_END;
    }

    public static Styler customizeStyle() {
        return new Styler() {
            @Override
            public List<String> styleCell(Column column, int row, int col, List<String> data) {
                if (col != 0) {
                    return data;
                }
                return data.stream().map(line -> {
                    if (col == 0) {
                        return createLink(String.format(RULE_REPO_URL, line.trim()), line);
                    } else {
                        return line;
                    }
                }).collect(Collectors.toList());
            }

            @Override
            public List<String> styleHeader(Column column, int col, List<String> data) {
                return data.stream().map(line -> GREEN + line + RESET).collect(Collectors.toList());
            }
        };
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