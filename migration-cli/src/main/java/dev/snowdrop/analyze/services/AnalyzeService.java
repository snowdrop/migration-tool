package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.service.scanner.CodeScannerService;
import dev.snowdrop.service.scanner.ScanCommandExecutor;
import dev.snowdrop.service.scanner.ScanningResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeService {

	private final Config config;
	private final CodeScannerService codeScannerService;

	public AnalyzeService(Config config) {
		this.config = config;
		this.codeScannerService = new CodeScannerService(config, new ScanCommandExecutor());
	}

	/**
	 * Analyzes code from rules using dynamic scanner selection.
	 * Unlike analyzeCodeFromRule, this method doesn't select a scanner upfront.
	 * Instead, it dynamically selects the appropriate scanner for each query
	 * during iteration based on the query-scanner mapping configuration.
	 *
	 * @param rules the migration rules to analyze
	 * @return map of rule ID to migration tasks with analysis results
	 */
	public Map<String, MigrationTask> analyze(List<Rule> rules) {
		Map<String, MigrationTask> tasks = new HashMap<>();

		for (Rule rule : rules) {
			ScanningResult scanningResult = codeScannerService.scan(rule);
			Map<String, List<Match>> results = scanningResult.isMatchSucceeded()
					? scanningResult.getMatches()
					: Collections.emptyMap();

			tasks.put(rule.ruleID(), new MigrationTask().withRule(rule).withMatchResults(results.get(rule.ruleID()))
					.withInstruction(rule.instructions()));
		}

		return tasks;
	}

}
