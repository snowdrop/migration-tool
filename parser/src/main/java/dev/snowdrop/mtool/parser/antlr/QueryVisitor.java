// Generated from Query.g4 by ANTLR 4.13.2

package dev.snowdrop.mtool.parser.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link QueryParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface QueryVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link QueryParser#searchQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchQuery(QueryParser.SearchQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrOperation(QueryParser.OrOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SimpleClause}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleClause(QueryParser.SimpleClauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndOperation}
	 * labeled alternative in {@link QueryParser#operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndOperation(QueryParser.AndOperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClause(QueryParser.ClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#fileType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileType(QueryParser.FileTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#symbol}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbol(QueryParser.SymbolContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#keyValuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyValuePair(QueryParser.KeyValuePairContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(QueryParser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(QueryParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link QueryParser#logicalOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOp(QueryParser.LogicalOpContext ctx);
}