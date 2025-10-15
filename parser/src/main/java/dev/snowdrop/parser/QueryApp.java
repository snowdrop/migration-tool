package dev.snowdrop.parser;

import dev.snowdrop.parser.antlr.QueryLexer;
import dev.snowdrop.parser.antlr.QueryParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class QueryApp {

    public static void main(String[] args) {
        QueryApp app = new QueryApp();
        String query;
        QueryVisitor visitor;

        System.out.println("=== Simple query with one clause ");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication')";
        visitor = app.parseQuery(query);
        visitor.getSimpleQueries().forEach(q -> app.logQueryResult(q));

        System.out.println("=========================");
        System.out.println("=== Query with clause AND clause ");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')");
        query = "FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')";
        visitor = app.parseQuery(query);
        visitor.getAndQueries().forEach(q -> app.logQueryResult(q));

        System.out.println("=========================");
        System.out.println("=== Query with clause OR clause OR clause");
        System.out.println("=== FIND java.annotation WHERE (name='@SpringBootApplication1') OR java.annotation WHERE (name='@SpringBootApplication2') OR java.annotation WHERE (name='@SpringBootApplication3')");
        query = """
        FIND java.annotation WHERE (name='@SpringBootApplication1') OR
        java.annotation WHERE (name='@SpringBootApplication2') OR
        java.annotation WHERE (name='@SpringBootApplication3')
        """;

        visitor = app.parseQuery(query);
        visitor.getOrQueries().forEach(q -> app.logQueryResult(q));
    }

    private void logQueryResult(Query qr) {
        System.out.println("=== Query parsed ===");
        System.out.println(String.format("=== Type: %s", qr.getFileType()));
        System.out.println(String.format("=== Symbol: %s", qr.getSymbol()));
        qr.getKeyValues().forEach((k, v) -> {
            System.out.println(String.format("Key: %s, value: %s.", k, v));
        });
    }

    private QueryVisitor parseQuery(String query) {
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