package dev.snowdrop.mtool.mcp;

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

    @Tool(description = "Analyze the source to discover the java classes, properties, etc files")
    public String sourceAnalyze(
            @ToolArg(description = "Query to perform on the project") String userQuery,
            @ToolArg(description = "Project path", defaultValue = ".") String projectPath) {
        Config cfg = new Config(projectPath, null, null, null, null, null, null, false, null, "treesitter", null);

        LOG.infof("Scan the project: %s using as query: %s", projectPath, userQuery);
        String[] param = userQuery.split(" ");
        if (param.length < 3) {
            return "Error: query must have 3 parts: <fileType> <symbol> <pattern>";
        }
        Query q = new Query(param[0], param[1], param[2], Collections.emptyMap());
        List<Result> matches = scanProject(cfg, q);

        if (matches.isEmpty()) {
            return "No matches found for query: " + userQuery;
        }

        String matchList = matches.stream()
                .map(m -> m.result().toString())
                .collect(Collectors.joining("\n"));

        return "Found " + matches.size() + " match(es):\n" + matchList;
    }

    private List<Result> scanProject(Config config, Query q) {
        ScanCommandExecutor scanCommandExecutor = new ScanCommandExecutor();
        return scanCommandExecutor.executeCommandForQuery(config, q);
    }
}
