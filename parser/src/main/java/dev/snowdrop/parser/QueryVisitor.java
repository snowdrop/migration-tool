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

    @Override
    public Set<Query> visitOrOperation(QueryParser.OrOperationContext ctx) {
        // System.out.println("!!! visitOrOperation called !!!");
        List<QueryParser.OperationContext> OrOps = ctx.operation();
        OrOps.forEach(orOp -> {
            // Recursively visit each operand - it might be SimpleClause or another Operation
            Set<Query> childQueries = visit(orOp);
            if (childQueries != null) {
                orQueries.addAll(childQueries);
            }
        });
        return orQueries;
    }

    @Override
    public Set<Query> visitAndOperation(QueryParser.AndOperationContext ctx) {
        // System.out.println("!!! visitAndOperation called !!!");
        List<QueryParser.OperationContext> AndOps = ctx.operation();
        AndOps.forEach(andOp -> {
            // Recursively visit each operand - it might be SimpleClause or another Operation
            Set<Query> childQueries = visit(andOp);
            if (childQueries != null) {
                andQueries.addAll(childQueries);
            }
        });
        return andQueries;
    }

    @Override
    public Set<Query> visitSimpleClause(QueryParser.SimpleClauseContext ctx) {
        QueryParser.ClauseContext cctx = ctx.clause();

        Map<String, String> keyValuePairs = cctx.keyValuePair().stream()
            .collect(Collectors.toMap(
                kvp -> kvp.key().getText(),
                kvp -> kvp.value().getText().replaceAll("'", "") // Remove quotes from value
            ));

        Query qr = new Query(
            cctx.fileType().getText(),
            cctx.symbol().getText(),
            keyValuePairs);
        simpleQueries.add(qr);

        return simpleQueries;
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