package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.model.Query;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeScannerService {
	private static final Logger logger = Logger.getLogger(CodeScannerService.class);
	private final Config config;
	private final ScanCommandExecutor scanCommandExecutor;

	public CodeScannerService(Config config, ScanCommandExecutor scanCommandExecutor) {
		this.config = config;
		this.scanCommandExecutor = scanCommandExecutor;
	}

	public ScanningResult scan(Rule rule) {

		/*
		   Check for precondition before processing the main condition
		   If the precondition succeeded, then we continue to parse the rules
		   Otherwise we throw an exception to tell to the user that we cannot analyze the application
		 */
		if (rule.when().precondition() != null && !rule.when().precondition().trim().isEmpty()) {
			QueryVisitor preconditionVisitor = QueryUtils.parseAndVisit(rule.when().precondition());
			ScanningResult preconditionResult = executeQueryWithVisitor(preconditionVisitor, rule);

			if (preconditionResult.isMatchSucceeded()) {
				logger.warnf("Precondition matched for rule %s: %s", rule.ruleID(), rule.when().precondition());
			}

			if (!preconditionResult.isMatchSucceeded()) {
				throw new PreconditionFailedException("Project does not meet the precondition requirements for rule: "
						+ rule.ruleID() + ". Precondition query: " + rule.when().precondition());
			}
		}

		return executeQueryWithVisitor(QueryUtils.parseAndVisit(rule.when().condition()), rule);
	}

	/**
	 * Executes the query using the visitor pattern and returns the scanning result.
	 * This method handles the different types of queries (simple, OR, AND) and their execution logic.
	 *
	 * @param visitor the QueryVisitor containing the parsed query information
	 * @param rule the rule being processed
	 * @return ScanningResult containing the match status and results
	 */
	private ScanningResult executeQueryWithVisitor(QueryVisitor visitor, Rule rule) {
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
			matchSucceeded = results.get(rule.ruleID()).stream().anyMatch(java.util.Objects::nonNull);
		} else if (!visitor.getAndQueries().isEmpty()) {
			boolean allMatched = true;
			List<Match> andMatches = new ArrayList<>();

			for (Query q : visitor.getAndQueries()) {
				List<Match> partial = scanCommandExecutor.executeQueryCommand(config, Set.of(q));
				andMatches.addAll(partial);

				// If any subquery has no results, the AND fails.
				if (partial.isEmpty()) {
					allMatched = false;
				}
			}

			results.merge(rule.ruleID(), andMatches, (existing, newOnes) -> {
				List<Match> combined = new ArrayList<>(existing);
				combined.addAll(newOnes);
				return combined;
			});

			matchSucceeded = allMatched;
		} else {
			logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
			results.put(rule.ruleID(), Collections.emptyList());
		}
		return new ScanningResult(matchSucceeded, results);
	}

}
