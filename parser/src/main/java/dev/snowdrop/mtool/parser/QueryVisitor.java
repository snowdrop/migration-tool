package dev.snowdrop.mtool.parser;

import dev.snowdrop.mtool.parser.antlr.QueryBaseVisitor;
import dev.snowdrop.mtool.parser.antlr.QueryParser;
import dev.snowdrop.mtool.model.parser.Query;

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
        String operation = "";

        if (cctx.FIND() != null) {
            boolean hasAll = cctx.getToken(QueryParser.T__0, 0) != null;
            operation = hasAll ? "find all" : "find";
        }

        QueryParser.ValueOrPairsContext vopCtx = cctx.valueOrPairs();
        if (vopCtx != null) {
            if (!vopCtx.keyValuePair().isEmpty()) {
                keyValuePairs = vopCtx.keyValuePair().stream()
                        .collect(Collectors.toMap(kvp -> kvp.key().getText(), kvp -> removeQuotes(kvp.value().getText())));
            } else if (vopCtx.value() != null) {
                String value = removeQuotes(vopCtx.value().getText());
                String symbol = cctx.symbol() != null ? cctx.symbol().getText() : "";
                String defaultKey = getDefaultKeyForSymbol(symbol);
                keyValuePairs.put(defaultKey, value);
            }
        }

        Query qr = new Query(cctx.fileType().getText(), cctx.symbol() != null ? cctx.symbol().getText() : "", operation,
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