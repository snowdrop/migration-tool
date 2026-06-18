package dev.snowdrop.mtool.commands;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.parser.QueryVisitor;
import dev.snowdrop.mtool.parser.antlr.QueryLexer;
import dev.snowdrop.mtool.parser.antlr.QueryParser;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;

@CommandLine.Command(name = "scan", description = "Scan an application against a query")
public class ScannerCommand implements Runnable {
    private static final Logger logger = Logger.getLogger(ScannerCommand.class);

    @CommandLine.Parameters(index = "0", description = "Path to the Java project to analyze")
    public String appPath;

    @CommandLine.Parameters(index = "1", description = "Query to be executed")
    public String query;

    @Override
    public void run() {
        long startTime = System.nanoTime();

        Path path = Paths.get(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            throw new IllegalStateException("❌ Project path of the application does not exist: " + appPath);
        }
        String appPathString = resolvePath(appPath).toString();
        Config config = new Config(appPathString, null, null, null, null, null, null, false, null, null, null);

        Query q;

        if (!query.isBlank()) {
            // pom.dependency is (gavs='org.springframework.boot:spring-boot-starter-parent:3.5.3')
            Optional<Query> userQuery = parseQuery(query).getSimpleQueries().stream().findFirst();
            q = userQuery.get();
            logger.infof("Processing user's query: %s", query);
        } else {
            q = new Query("pom", "dependency", Map.of(
                    "gavs", "org.springframework.boot:spring-boot-starter-parent:3.5.3"));
        }

        // Get an instance of the Scanner
        ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
        List<Match> matches = scanCommandExecutor.executeCommandForQuery(config, q);

        matches.forEach(m -> {
            logger.infof("Result : %s ", m.result());
        });

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        logger.infof(matches.size() + " match(es). Elapsed: " + elapsedMs + " ms");
    }

    QueryVisitor parseQuery(String query) {
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
