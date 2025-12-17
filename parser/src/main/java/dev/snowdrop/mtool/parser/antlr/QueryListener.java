// Generated from Query.g4 by ANTLR 4.13.2

package dev.snowdrop.mtool.parser.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link QueryParser}.
 */
public interface QueryListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link QueryParser#searchQuery}.
	 * @param ctx the parse tree
	 */
	void enterSearchQuery(QueryParser.SearchQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#searchQuery}.
	 * @param ctx the parse tree
	 */
	void exitSearchQuery(QueryParser.SearchQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OrOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void enterOrOperation(QueryParser.OrOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OrOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void exitOrOperation(QueryParser.OrOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SimpleClause}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void enterSimpleClause(QueryParser.SimpleClauseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SimpleClause}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void exitSimpleClause(QueryParser.SimpleClauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AndOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void enterAndOperation(QueryParser.AndOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AndOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 */
	void exitAndOperation(QueryParser.AndOperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#clause}.
	 * @param ctx the parse tree
	 */
	void enterClause(QueryParser.ClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#clause}.
	 * @param ctx the parse tree
	 */
	void exitClause(QueryParser.ClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#fileType}.
	 * @param ctx the parse tree
	 */
	void enterFileType(QueryParser.FileTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#fileType}.
	 * @param ctx the parse tree
	 */
	void exitFileType(QueryParser.FileTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#symbol}.
	 * @param ctx the parse tree
	 */
	void enterSymbol(QueryParser.SymbolContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#symbol}.
	 * @param ctx the parse tree
	 */
	void exitSymbol(QueryParser.SymbolContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#keyValuePair}.
	 * @param ctx the parse tree
	 */
	void enterKeyValuePair(QueryParser.KeyValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#keyValuePair}.
	 * @param ctx the parse tree
	 */
	void exitKeyValuePair(QueryParser.KeyValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#key}.
	 * @param ctx the parse tree
	 */
	void enterKey(QueryParser.KeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#key}.
	 * @param ctx the parse tree
	 */
	void exitKey(QueryParser.KeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(QueryParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(QueryParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link QueryParser#logicalOp}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOp(QueryParser.LogicalOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link QueryParser#logicalOp}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOp(QueryParser.LogicalOpContext ctx);
}