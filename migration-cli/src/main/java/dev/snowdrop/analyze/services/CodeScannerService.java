package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeScannerService {
	private static final Logger logger = Logger.getLogger(CodeScannerService.class);
	private final Config config;
	private final ScanCommandExecutor scanCommandExecutor;

	public CodeScannerService(Config config, ScanCommandExecutor scanCommandExecutor) {
		this.config = config;
		this.scanCommandExecutor = scanCommandExecutor;
	}

	public ScanningResult scan(Rule rule) {
		// Parse first the Rule condition to populate the Query object using the YAML condition query
		// See the parser maven project for examples, unit tests
		QueryVisitor visitor = QueryUtils.parseAndVisit(rule.when().condition());
		Map<String, List<Match>> results = new HashMap<>();
		boolean matchSucceeded = false;

		/*
		 * Handle the 3 supported cases where the query contains:
		 *
		 * - One clause: java.annotation is '@SpringBootApplication'
		 *
		 * - Clauses separated with the OR operator:
		 *
		 * java.annotation is '@SpringBootApplication' OR java.annotation is '@Deprecated'
		 *
		 * - Clauses separated with the AND operator:
		 *
		 * java.annotation is '@SpringBootApplication' AND pom.dependency is (groupId='org.springframework.boot',
		 * artifactId='spring-boot', version='3.4.2')
		 *
		 * See grammar definition:
		 * https://raw.githubusercontent.com/snowdrop/migration-tool/refs/heads/main/parser/src/main/antlr4/Query.g4
		 */

		if (!visitor.getSimpleQueries().isEmpty()) {
			results.put(rule.ruleID(), scanCommandExecutor.executeQueryCommand(config, visitor.getSimpleQueries()));
			matchSucceeded = !results.get(rule.ruleID()).isEmpty();
		} else if (!visitor.getOrQueries().isEmpty()) {
			results.put(rule.ruleID(), scanCommandExecutor.executeQueryCommand(config, visitor.getOrQueries()));
			matchSucceeded = results.get(rule.ruleID()).stream().anyMatch(r -> r != null);
		} else if (!visitor.getAndQueries().isEmpty()) {
			List<Match> allMatches = results.put(rule.ruleID(),
					scanCommandExecutor.executeQueryCommand(config, visitor.getAndQueries()));
			results.put(rule.ruleID(), allMatches);
		} else {
			logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
			results.put(rule.ruleID(), Collections.emptyList());
		}
		return new ScanningResult(matchSucceeded, results);
	}

}
