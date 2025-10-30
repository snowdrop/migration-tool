package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.JdtLsClient;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import org.eclipse.lsp4j.SymbolInformation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdtLsScanner implements CodeScanner {
    private final Config config;
    private final JdtLsClient jdtLsClient;
    private boolean started = false;

    public JdtLsScanner(Config config, JdtLsClient jdtLsClient) {
        this.config = config;
        this.jdtLsClient = jdtLsClient;
    }

    @Override
    public Map<String, MigrationTask> analyze(List<Rule> rules) throws IOException {
        // Lazy initialization
        if (!started) {
            try {
                jdtLsClient.launchLsProcess();
                jdtLsClient.createLaunchLsClient();
                jdtLsClient.initLanguageServer();
                started = true;
            } catch (Exception e) {
                throw new IOException("Failed to start JDT-LS client", e);
            }
        }

        Map<String, MigrationTask> tasks = new HashMap<>();

        for (Rule rule : rules) {
            Map<String, List<SymbolInformation>> results = jdtLsClient.executeLsCmd(rule);
            tasks.put(rule.ruleID(), new MigrationTask().withRule(rule).withLsResults(results.get(rule.ruleID()))
                    .withInstruction(rule.instructions()));
        }
        close();
        return tasks;
    }

    public void close() {
        if (jdtLsClient != null && started) {
            jdtLsClient.stop();
            started = false;
        }
    }
}