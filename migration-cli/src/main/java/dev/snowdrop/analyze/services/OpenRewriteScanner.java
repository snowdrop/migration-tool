package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rewrite;
import dev.snowdrop.analyze.model.Rule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenRewriteScanner implements CodeScanner {
	private final Config config;
	private final RewriteService rewriteService;

	public OpenRewriteScanner(Config config, RewriteService rewriteService) {
		this.config = config;
		this.rewriteService = rewriteService;
	}

	@Override
	public Map<String, MigrationTask> analyze(List<Rule> rules) throws IOException {
		Map<String, MigrationTask> tasks = new HashMap<>();

		for (Rule rule : rules) {
			Map<String, List<Rewrite>> results = rewriteService.executeRewriteCmd(config, rule);
			tasks.put(rule.ruleID(), new MigrationTask().withRule(rule).withRewriteResults(results.get(rule.ruleID()))
					.withInstruction(rule.instructions()));
		}

		return tasks;
	}

}
