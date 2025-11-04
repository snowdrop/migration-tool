package dev.snowdrop.parser;

import dev.snowdrop.parser.antlr.QueryLexer;
import dev.snowdrop.parser.antlr.QueryParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class QueryUtils {

	/*

	 */
	public static QueryVisitor parseAndVisit(String query) {
		try {
			ANTLRInputStream input = new ANTLRInputStream(query);
			QueryLexer lexer = new QueryLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);

			QueryParser parser = new QueryParser(tokens);
			ParseTree tree = parser.searchQuery();

			// Create and use the visitor
			QueryVisitor visitor = new QueryVisitor();
			visitor.visit(tree);

			return visitor;
		} catch (Exception e) {
			System.err.println("Error parsing query: " + query);
			e.printStackTrace();
			return new QueryVisitor();
		}
	}
}
