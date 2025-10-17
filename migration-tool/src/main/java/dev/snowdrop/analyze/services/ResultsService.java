package dev.snowdrop.analyze.services;

import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import dev.snowdrop.analyze.model.MigrationTask;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;
import java.util.Map;

public class ResultsService {

    public static void showCsvTable(Map<String, MigrationTask> results, String source, String target) {
        // TODO: Test https://github.com/freva/ascii-table to see if the url to the file is not truncated
        AsciiTable at = new AsciiTable();
        at.getContext().setWidth(220); // Set overall table width
        at.addRule();

        AT_Row row;
        row = at.addRow("Rule ID", "Source to Target", "Found", "Information Details");
        row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
        row.getCells().get(1).getContext().setTextAlignment(TextAlignment.LEFT);
        row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);
        row.getCells().get(3).getContext().setTextAlignment(TextAlignment.LEFT);

        at.addRule();
        at.getRenderer().setCWC(new CWC_FixedWidth().add(40).add(25).add(5).add(130));

        for (Map.Entry<String, MigrationTask> entry : results.entrySet()) {
            String ruleId = entry.getKey();
            MigrationTask aTask = entry.getValue();
            List<SymbolInformation> queryResults = aTask.getResults();
            String hasQueryResults = queryResults.isEmpty() ? "No" : "Yes";
            String sourceToTarget = String.format("%s -> %s", source, target);

            if (queryResults.isEmpty()) {
                row = at.addRow(ruleId, sourceToTarget, hasQueryResults, "No symbols found");
                row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(1).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);
                row.getCells().get(3).getContext().setTextAlignment(TextAlignment.LEFT);
            } else {
                // Add first symbol
                SymbolInformation firstSymbol = queryResults.get(0);
                String firstSymbolDetails = formatSymbolInformation(firstSymbol);
                row = at.addRow(ruleId, sourceToTarget, hasQueryResults, firstSymbolDetails + "\n" + queryResults.get(0).getLocation().getUri());
                row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(1).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);
                row.getCells().get(3).getContext().setTextAlignment(TextAlignment.LEFT);

                // Add additional symbols in subsequent rows with empty rule id and found columns
                for (int i = 1; i < queryResults.size(); i++) {
                    String symbolDetails = formatSymbolInformation(queryResults.get(i));
                    row = at.addRow("", "", 33, symbolDetails + "\n" + queryResults.get(0).getLocation().getUri());
                    row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                    row.getCells().get(1).getContext().setTextAlignment(TextAlignment.LEFT);
                    row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);
                    row.getCells().get(3).getContext().setTextAlignment(TextAlignment.LEFT);
                }
            }
            at.addRule();
        }

        // Use System.out.println instead of logger to avoid log formatting
        System.out.println("\n=== Code Analysis Results ===");
        System.out.println(at.render());
    }

    private static String formatSymbolInformation(SymbolInformation si) {
        return String.format("Found %s at line %s, char: %s - %s",
            si.getName(),
            si.getLocation().getRange().getStart().getLine() + 1,
            si.getLocation().getRange().getStart().getCharacter(),
            si.getLocation().getRange().getEnd().getCharacter()
        );
    }

}
