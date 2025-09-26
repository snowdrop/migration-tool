package dev.snowdrop.analyze.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import dev.snowdrop.analyze.JdtLsFactory;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.snowdrop.analyze.utils.RuleUtils.getLocationCode;
import static dev.snowdrop.analyze.utils.RuleUtils.getLocationName;
import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFolder;

import de.vandermeer.asciitable.AsciiTable;

public class LsSearchService {

    private static final Logger logger = Logger.getLogger(LsSearchService.class);

    public static Map<String, List<SymbolInformation>> executeLsCmd(JdtLsFactory factory, Rule rule) {
        Map<String, List<SymbolInformation>> ruleResults = new HashMap<>();

        // Log the LS Query command to be executed on the LS server
        logger.infof("==== CLIENT: Sending the command '%s' ...", factory.lsCmd);

        // Handle three cases: single java.referenced, OR conditions, AND conditions
        if (rule.when().or() != null && !rule.when().or().isEmpty()) {
            logger.infof("Rule When includes: %s between java.referenced", "OR");
            rule.when().or().forEach(condition -> {
                List<SymbolInformation> results = executeCommandForCondition(factory, rule, condition.javaReferenced());
                ruleResults.putAll(Map.of(rule.ruleID(),results));
            });
        } else if (rule.when().and() != null && !rule.when().and().isEmpty()) {
            logger.infof("Rule When includes: %s between java.referenced", "AND");
            rule.when().and().forEach(condition -> {
                List<SymbolInformation> results = executeCommandForCondition(factory, rule, condition.javaReferenced());
                ruleResults.putAll(Map.of(rule.ruleID(),results));
            });
        } else if (rule.when().javaReferenced() != null) {
            logger.infof("Rule When includes: single java.referenced");
            List<SymbolInformation> results = executeCommandForCondition(factory, rule, rule.when().javaReferenced());
            ruleResults.putAll(Map.of(rule.ruleID(),results));
        } else {
            logger.warnf("Rule %s has no valid java.referenced conditions", rule.ruleID());
            ruleResults.put(rule.ruleID(), new ArrayList<>());
        }

        return ruleResults;
    }

    private static List<SymbolInformation> executeCommandForCondition(JdtLsFactory factory, Rule rule, Rule.JavaReferenced javaReferenced) {
        var paramsMap = Map.of(
            "project", "java", // hard coded value to java within the analyzer java external-provider
            "location", getLocationCode(javaReferenced.location()),
            "query", javaReferenced.pattern(), // pattern from the rule
            "analysisMode", "source-only" // 2 modes are supported: source-only and full
        );

        List<Object> cmdArguments = List.of(paramsMap);

        try {
            CompletableFuture<List<SymbolInformation>> symbolsFuture = factory.future
                .thenApplyAsync(ignored -> executeCmd(factory, rule, cmdArguments))
                .exceptionally(throwable -> {
                    logger.errorf("Error executing LS command for rule %s: %s", rule.ruleID(), throwable.getMessage(), throwable);
                    return new ArrayList<SymbolInformation>();
                });

            return symbolsFuture.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            logger.errorf("Failed to execute command for rule %s: %s", rule.ruleID(), e.getMessage());
            return null;
        }
    }

    public static List<SymbolInformation> executeCmd(JdtLsFactory factory, Rule rule, List<Object> arguments) {
        List<Object> cmdArguments = (arguments != null && !arguments.isEmpty())
            ? arguments
            : Collections.EMPTY_LIST;

        ExecuteCommandParams commandParams = new ExecuteCommandParams(
            factory.lsCmd,
            cmdArguments
        );

        CompletableFuture<Object> commandResult = factory.remoteProxy.getWorkspaceService()
            .executeCommand(commandParams)
            .exceptionally( t -> {
                    t.printStackTrace();
                    return null;
                });

        Object result = commandResult.join();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<SymbolInformation> symbolInformationList = new ArrayList<>();

        if (result != null) {
            logger.infof("==== CLIENT: --- Command params: %s.", commandParams);
            logger.infof("==== CLIENT: --- Search Results found for rule: %s.", rule.ruleID());
            logger.infof("==== CLIENT: --- JSON response: %s",gson.toJson(result));

            try {
                if (result instanceof List) {
                    List<?> resultList = (List<?>) result;
                    for (Object item : resultList) {
                        SymbolInformation symbol = new SymbolInformation();

                        // Extract data from the result object and populate SymbolInformation
                        // This assumes the result contains objects with name, kind, and location data
                        if (item instanceof java.util.Map) {
                            java.util.Map<?, ?> itemMap = (java.util.Map<?, ?>) item;

                            // Set name if available
                            if (itemMap.containsKey("name")) {
                                symbol.setName(String.valueOf(itemMap.get("name")));
                            }

                            // Set kind if available (convert to SymbolKind)
                            if (itemMap.containsKey("kind")) {
                                Object kindValue = itemMap.get("kind");
                                if (kindValue instanceof Number) {
                                    symbol.setKind(SymbolKind.forValue(((Number) kindValue).intValue()));
                                }
                            }

                            // Set location if available
                            if (itemMap.containsKey("location")) {
                                // Parse location data - this would need to be adapted based on actual structure
                                Object locationData = itemMap.get("location");
                                Location location = gson.fromJson(gson.toJson(locationData), Location.class);
                                symbol.setLocation(location);
                            }

                            symbolInformationList.add(symbol);
                        }
                    }
                } else {
                    // Fallback to direct GSON conversion if result is not a List
                    Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {}.getType();
                    symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
                }
            } catch (Exception e) {
                logger.warnf("==== CLIENT: Failed to create SymbolInformation objects: %s", e.getMessage());
                // Fallback to GSON conversion
                try {
                    Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {}.getType();
                    symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
                } catch (JsonSyntaxException | ClassCastException ex) {
                    logger.warnf("==== CLIENT: Failed fallback GSON conversion: %s", ex.getMessage());
                }
            }

            if (symbolInformationList.isEmpty()) {
                logger.infof("==== CLIENT: SymbolInformation List is empty.");
            } else {
                Map<String, Object> args = (Map<String, Object>) arguments.get(0);
                logger.infof("==== CLIENT: Found %s usage(s) of symbol: %s, name: %s", symbolInformationList.size(), getLocationName(args.get("location").toString()),args.get("query"));
                for (SymbolInformation si : symbolInformationList) {
                    logger.debugf("==== CLIENT: Found %s at line %s, char: %s - %s within the file: %s)",
                        si.getName(),
                        si.getLocation().getRange().getStart().getLine() + 1,
                        si.getLocation().getRange().getStart().getCharacter(),
                        si.getLocation().getRange().getEnd().getCharacter(),
                        si.getLocation().getUri()
                    );
                }
            }
            logger.infof("==== CLIENT: ----------------------");
        } else {
            logger.warn("==== CLIENT: Received null result for command.");
        }

        return symbolInformationList;
    }

    public static Map<String, MigrationTask> analyzeCodeFromRule(JdtLsFactory factory) throws IOException {
        List<Rule> rules = parseRulesFromFolder(factory.rulesPath);
        Map<String, MigrationTask> ruleMigrationTasks = new HashMap<>();

        // Collect all results from all the rule's queries executed
        for (Rule rule : rules) {
            Map<String, List<SymbolInformation>> ruleResults = executeLsCmd(factory, rule);
            ruleMigrationTasks.putAll(Map.of(
                rule.ruleID(),
                new MigrationTask()
                    .withRule(rule)
                    .withResults(ruleResults.get(rule.ruleID()))
                    ));
        }

        // Display the rule queries results
        displayResultsTable(ruleMigrationTasks);

        return ruleMigrationTasks;
    }

    private static void displayResultsTable(Map<String, MigrationTask> results) {
        // TODO: Test https://github.com/freva/ascii-table to see if the url to the file is not truncated
        AsciiTable at = new AsciiTable();
        at.getContext().setWidth(180); // Set overall table width
        at.addRule();

        AT_Row row;
        row = at.addRow("Rule ID", "Found", "Information Details");
        row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
        row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
        row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);

        at.addRule();
        at.getRenderer().setCWC(new CWC_FixedWidth().add(45).add(5).add(130));

        for (Map.Entry<String, MigrationTask> entry : results.entrySet()) {
            String ruleId = entry.getKey();
            MigrationTask aTask = entry.getValue();
            List<SymbolInformation> queryResults = aTask.getResults();
            String hasQueryResults = queryResults.isEmpty() ? "No" : "Yes";

            if (queryResults.isEmpty()) {
                row = at.addRow(ruleId, hasQueryResults, "No symbols found");
                row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
                row.getCells().get(2).getContext().setTextAlignment(TextAlignment.LEFT);
            } else {
                // Add first symbol
                SymbolInformation firstSymbol = queryResults.get(0);
                String firstSymbolDetails = formatSymbolInformation(firstSymbol);
                row = at.addRow(ruleId, hasQueryResults, firstSymbolDetails + "\n" + queryResults.get(0).getLocation().getUri());
                row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
                row.getCells().get(2).getContext().setTextAlignment(TextAlignment.LEFT);

                // Add additional symbols in subsequent rows with empty rule id and found columns
                for (int i = 1; i < queryResults.size(); i++) {
                    String symbolDetails = formatSymbolInformation(queryResults.get(i));
                    row = at.addRow("", "", symbolDetails + "\n" + queryResults.get(0).getLocation().getUri());
                    row.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
                    row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
                    row.getCells().get(2).getContext().setTextAlignment(TextAlignment.LEFT);
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