package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
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
			MigrationTask task = processRule(rule, codeScannerService);
			tasks.put(rule.ruleID(), task);
		}

		return tasks;
	}

	/**
	 * Analyze a single rule and return the migration task result.
	 *
	 * @param rule the rule to analyze
	 * @return a map containing the rule ID as key and migration task as value
	 * @throws IOException if an error occurs during analysis
	 */
	public Map<String, MigrationTask> analyze(Rule rule) throws IOException {
		ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
		CodeScannerService codeScannerService = new CodeScannerService(config, scanCommandExecutor);

		MigrationTask task = processRule(rule, codeScannerService);

		Map<String, MigrationTask> result = new HashMap<>();
		result.put(rule.ruleID(), task);
		return result;
	}

	/**
	 * Processes a single rule and creates a migration task with the scanning results.
	 *
	 * @param rule the rule to process
	 * @param codeScannerService the service to use for scanning
	 * @return the migration task with the analysis results
	 */
	private MigrationTask processRule(Rule rule, CodeScannerService codeScannerService) {
		ScanningResult scanningResult = codeScannerService.scan(rule);
		List<Match> matches = scanningResult.isMatchSucceeded()
				? scanningResult.getMatches().get(rule.ruleID())
				: Collections.emptyList();

		return new MigrationTask().withRule(rule).withMatchResults(matches).withInstruction(rule.instructions());
	}

}
