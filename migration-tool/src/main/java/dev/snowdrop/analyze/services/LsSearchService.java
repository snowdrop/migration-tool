package dev.snowdrop.analyze.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.analyze.JdtLsFactory;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.model.Query;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.jboss.logging.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.snowdrop.analyze.utils.RuleUtils.getLocationCode;
import static dev.snowdrop.analyze.utils.RuleUtils.getLocationName;

public class LsSearchService {

    private static final Logger logger = Logger.getLogger(LsSearchService.class);

    public static Map<String, List<SymbolInformation>> executeLsCmd(JdtLsFactory factory, Rule rule) {
        Map<String, List<SymbolInformation>> ruleResults = new HashMap<>();

        // Log the LS Query command to be executed on the LS server
        logger.infof("==== CLIENT: Sending the command '%s' ...", factory.lsCmd);

        // Parse first the Rule condition to populate the Query object using the YAML Condition query
        // See the parser maven project for examples, unit tests
        QueryVisitor visitor = QueryUtils.parseAndVisit(rule.when().Condition());

        /*
           Handle the 3 supported cases where the query contains:

           - One clause: FIND java.annotation WHERE (name='@SpringBootApplication')

           - Clauses separated with the OR operator:

             FIND java.annotation WHERE (name='@SpringBootApplication') OR
                  java.annotation WHERE (name='@Deprecated')

           - Clauses separated with the AND operator:

             FIND java.annotation WHERE (name='@SpringBootApplication') AND
                  pom.dependency WHERE (groupId='org.springframework.boot', artifactId='spring-boot', version='3.4.2')

         */
        if (visitor.getSimpleQueries().size() == 1) {
            visitor.getSimpleQueries().stream().findFirst().ifPresent(q -> {
                List<SymbolInformation> results = executeQueryCommand(factory, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else if (visitor.getOrQueries().size() > 1) {
            visitor.getOrQueries().stream().forEach(q -> {
                List<SymbolInformation> results = executeQueryCommand(factory, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else if (visitor.getAndQueries().size() > 1) {
            visitor.getAndQueries().stream().forEach(q -> {
                List<SymbolInformation> results = executeQueryCommand(factory, rule, q);
                ruleResults.putAll(Map.of(rule.ruleID(), results));
            });
        } else {
            logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
            ruleResults.put(rule.ruleID(), new ArrayList<>());
        }


        /*
          Old code deprecated in favor of the Antlr parser !
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
        */

        return ruleResults;
    }


    private static List<SymbolInformation> executeQueryCommand(JdtLsFactory factory, Rule rule, Query q) {

        // Map the Query object with the RuleEntry parameters to be sent to the Language Server
        var paramsMap = Map.of(
            "project", q.fileType().toLowerCase(),  // This value should be java
            "location", getLocationCode(q.symbol()),    // The symbol should correspond to one of the value that LS supports: annotation, etc
            "query", q.keyValues().get("name"),         // TODO: To be improved as we need a mapper able to extract the k=v and convert them to the pattern
            "analysisMode", "source-only"               // 2 modes are supported: source-only and full
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

    @Deprecated
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
            .exceptionally(t -> {
                t.printStackTrace();
                return null;
            });

        Object result = commandResult.join();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<SymbolInformation> symbolInformationList = new ArrayList<>();

        if (result != null) {
            logger.infof("==== CLIENT: --- Command params: %s.", commandParams);
            logger.infof("==== CLIENT: --- Search Results found for rule: %s.", rule.ruleID());
            logger.infof("==== CLIENT: --- JSON response: %s", gson.toJson(result));

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
                    Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {
                    }.getType();
                    symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
                }
            } catch (Exception e) {
                logger.warnf("==== CLIENT: Failed to create SymbolInformation objects: %s", e.getMessage());
                // Fallback to GSON conversion
                try {
                    Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {
                    }.getType();
                    symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
                } catch (JsonSyntaxException | ClassCastException ex) {
                    logger.warnf("==== CLIENT: Failed fallback GSON conversion: %s", ex.getMessage());
                }
            }

            if (symbolInformationList.isEmpty()) {
                logger.infof("==== CLIENT: SymbolInformation List is empty.");
            } else {
                Map<String, Object> args = (Map<String, Object>) arguments.get(0);
                logger.infof("==== CLIENT: Found %s usage(s) of symbol: %s, name: %s", symbolInformationList.size(), getLocationName(args.get("location").toString()), args.get("query"));
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

}