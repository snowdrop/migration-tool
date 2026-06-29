package dev.snowdrop.mtool.parser;

import dev.snowdrop.mtool.parser.antlr.QueryLexer;
import dev.snowdrop.mtool.parser.antlr.QueryParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class QueryParserUtil {
    public QueryVisitor parseQuery(String query) {
        try {
            CharStream input = CharStreams.fromString(query);
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
