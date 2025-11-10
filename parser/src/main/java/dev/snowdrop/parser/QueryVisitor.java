package dev.snowdrop.parser;

import dev.snowdrop.parser.antlr.QueryBaseVisitor;
import dev.snowdrop.parser.antlr.QueryParser;
import dev.snowdrop.model.Query;

import java.util.*;
import java.util.stream.Collectors;

public class QueryVisitor extends QueryBaseVisitor<Set<Query>> {
	Set<Query> simpleQueries = new LinkedHashSet<>();
	Set<Query> andQueries = new LinkedHashSet<>();
	Set<Query> orQueries = new LinkedHashSet<>();
	private boolean inAndOperation = false;
	private boolean inOrOperation = false;

	@Override
	public Set<Query> visitOrOperation(QueryParser.OrOperationContext ctx) {
		List<QueryParser.OperationContext> OrOps = ctx.operation();
		boolean previousInOrOperation = inOrOperation;
		inOrOperation = true;

		OrOps.forEach(orOp -> {
			// Recursively visit each operand - it might be SimpleClause or another Operation
			Set<Query> childQueries = visit(orOp);
			if (childQueries != null) {
				orQueries.addAll(childQueries);
			}
		});

		inOrOperation = previousInOrOperation;
		return orQueries;
	}

	@Override
	public Set<Query> visitAndOperation(QueryParser.AndOperationContext ctx) {
		List<QueryParser.OperationContext> AndOps = ctx.operation();
		boolean previousInAndOperation = inAndOperation;
		inAndOperation = true;

		AndOps.forEach(andOp -> {
			// Recursively visit each operand - it might be SimpleClause or another Operation
			Set<Query> childQueries = visit(andOp);
			if (childQueries != null) {
				andQueries.addAll(childQueries);
			}
		});

		inAndOperation = previousInAndOperation;
		return andQueries;
	}

	@Override
	public Set<Query> visitSimpleClause(QueryParser.SimpleClauseContext ctx) {
		QueryParser.ClauseContext cctx = ctx.clause();
		Map<String, String> keyValuePairs = new HashMap<>();

		// Check if the clause has key-value pairs or just a single value
		if (cctx.keyValuePair() != null && !cctx.keyValuePair().isEmpty()) {
			// Handle case with key=value pairs: java.annotation is (name='@SpringBootApplication')
			keyValuePairs = cctx.keyValuePair().stream()
					.collect(Collectors.toMap(kvp -> kvp.key().getText(), kvp -> removeQuotes(kvp.value().getText())));
		} else if (cctx.value() != null) {
			// Handle case where there is no k=v but only a single value: java.annotation is '@SpringBootApplication'
			String value = removeQuotes(cctx.value().getText()); // Remove quotes from value
			String symbol = cctx.symbol() != null ? cctx.symbol().getText() : "";

			// Use appropriate default key based on the symbol type
			String defaultKey = getDefaultKeyForSymbol(symbol);
			keyValuePairs.put(defaultKey, value);
		}

		Query qr = new Query(cctx.fileType().getText(), cctx.symbol() != null ? cctx.symbol().getText() : "",
				keyValuePairs);

		// Only add to simpleQueries if this clause is not part of an AND or OR operation
		if (!inAndOperation && !inOrOperation) {
			simpleQueries.add(qr);
		}

		// Create a temporary set to return this single query
		Set<Query> result = new LinkedHashSet<>();
		result.add(qr);

		return result;
	}

	/**
	 * Returns an appropriate default key for a given symbol type when no explicit key is provided
	 */
	private String getDefaultKeyForSymbol(String symbol) {
		return switch (symbol.toLowerCase()) {
			case "annotation" -> "name";
			case "dependency" -> "artifactId";
			case "property" -> "name";
			case "class" -> "name";
			case "method" -> "name";
			case "field" -> "name";
			default -> "value"; // Generic fallback
		};
	}

	/**
	 * Removes both single and double quotes from the beginning and end of a string
	 */
	private String removeQuotes(String value) {
		if (value == null || value.length() < 2) {
			return value;
		}

		// Remove single quotes
		if (value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}

		// Remove double quotes
		if (value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}

		return value;
	}

	public Set<Query> getSimpleQueries() {
		return simpleQueries;
	}

	public Set<Query> getAndQueries() {
		return andQueries;
	}

	public Set<Query> getOrQueries() {
		return orQueries;
	}
}