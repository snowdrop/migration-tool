package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.JdtLsFactory;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;
import org.eclipse.lsp4j.SymbolInformation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.analyze.services.LsSearchService.executeLsCmd;
import static dev.snowdrop.analyze.services.RewriteService.executeRewriteCmd;

public class AnalyzeService {
    public static Map<String, MigrationTask> analyzeCodeFromRule(JdtLsFactory factory, String scanner, List<Rule> rules) throws IOException {
        Map<String, MigrationTask> ruleMigrationTasks = new HashMap<>();

        // Collect all results from all the rule's queries executed and add the instructions
        for (Rule rule : rules) {
            switch (scanner) {
                case "jdtls":
                    Map<String, List<SymbolInformation>> lsResults = executeLsCmd(factory, rule);

                    ruleMigrationTasks.putAll(Map.of(
                        rule.ruleID(),
                        new MigrationTask()
                            .withRule(rule)
                            .withLsResults(lsResults.get(rule.ruleID()))
                            .withInstruction(rule.instructions())
                    ));
                    break;

                case "openrewrite":
                    Map<String, List<Rewrite>> rewriteResults = executeRewriteCmd(factory, rule);

                    ruleMigrationTasks.putAll(Map.of(
                        rule.ruleID(),
                        new MigrationTask()
                            .withRule(rule)
                            .withRewriteResults(rewriteResults.get(rule.ruleID()))
                            .withInstruction(rule.instructions())
                    ));
                    break;
            }

        }
        return ruleMigrationTasks;
    }

    public enum Scanner {
        openrewrite,
        jdtLs
    }
}
