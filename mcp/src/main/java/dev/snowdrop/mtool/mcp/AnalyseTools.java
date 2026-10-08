package dev.snowdrop.mtool.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Result;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.scanner.ScanCommandExecutor;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import org.jboss.logging.Logger;

public class AnalyseTools {

    private static final Logger LOG = Logger.getLogger(AnalyseTools.class);

    @Tool(description = "Analyze the source to discover the java classes, annotations, properties, etc files. "
            + "Accepts multiple queries separated by semicolons, e.g. 'java class all;java annotation all'")
    public String sourceAnalyze(
            @ToolArg(description = "Queries to perform, separated by ';'. Each query has 3 parts: <fileType> <symbol> <operation>. "
                    + "Example: 'java class all;java annotation all';pom dependency all; properties all") String userQueries,
            @ToolArg(description = "Project path", defaultValue = ".") String projectPath) {

        Config cfg = new Config(projectPath, null, null, null, null, null, null, false, null, "treesitter", null);
        ScanCommandExecutor executor = new ScanCommandExecutor();

        String[] queryStrings = userQueries.split(";");
        List<String> sections = new ArrayList<>();
        int totalMatches = 0;

        for (String queryStr : queryStrings) {
            String trimmed = queryStr.trim();
            String[] param = trimmed.split(" ");
            if (param.length < 2) {
                sections.add("## Query: " + trimmed
                        + "\nError: query must have at least 2 parts: <fileType> [<symbol>] <operation>");
                continue;
            }

            Query q;
            if (param.length == 2) {
                q = new Query(param[0], "", param[1], Collections.emptyMap());
            } else {
                q = new Query(param[0], param[1], param[2], Collections.emptyMap());
            }
            LOG.infof("Scan the project: %s using query: %s", projectPath, trimmed);

            List<Result> matches = executor.executeCommandForQuery(cfg, q);
            totalMatches += matches.size();

            if (matches.isEmpty()) {
                sections.add("## Query: " + trimmed + "\nNo matches found");
            } else {
                String matchList = matches.stream()
                        .map(m -> m.result().toString())
                        .collect(Collectors.joining("\n"));
                sections.add("## Query: " + trimmed + "\nFound " + matches.size() + " match(es):\n" + matchList);
            }
        }

        return "Total: " + totalMatches + " match(es) across " + queryStrings.length + " query(ies)\n\n"
                + String.join("\n\n", sections);
    }
}
