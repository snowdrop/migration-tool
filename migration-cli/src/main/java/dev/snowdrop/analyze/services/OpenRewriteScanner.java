package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenRewriteScanner implements CodeScanner {
	private final Config config;

	public OpenRewriteScanner(Config config) {
		this.config = config;
	}

	@Override
	public Map<String, MigrationTask> analyze(List<Rule> rules) throws IOException {
		Map<String, MigrationTask> tasks = new HashMap<>();
		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		CodeScannerService codeScannerService = new CodeScannerService(config, scanCommandExecutor);

		for (Rule rule : rules) {
			ScanningResult scanningResult = codeScannerService.scan(rule);
			Map<String, List<Match>> results = scanningResult.getMatches();
			tasks.put(rule.ruleID(), new MigrationTask().withRule(rule).withMatchResults(results.get(rule.ruleID()))
					.withInstruction(rule.instructions()));
		}

		return tasks;
	}

}
