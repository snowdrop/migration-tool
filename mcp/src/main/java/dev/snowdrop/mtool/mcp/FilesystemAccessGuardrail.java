package dev.snowdrop.mtool.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.ToolInputGuardrail;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Singleton
public class FilesystemAccessGuardrail implements ToolInputGuardrail {

    private static final Logger LOG = Logger.getLogger(FilesystemAccessGuardrail.class);

    private static final List<String> PATH_ARG_NAMES = List.of("projectPath", "target", "source");

    @ConfigProperty(name = "mtool.allowed-dirs", defaultValue = ".")
    List<String> allowedDirs;

    @Override
    public void apply(ToolInputContext context) {
        List<Path> allowed = allowedDirs.stream()
                .map(d -> Paths.get(d).toAbsolutePath().normalize())
                .toList();

        JsonObject args = context.getArguments();
        for (String argName : PATH_ARG_NAMES) {
            String value = args.getString(argName);
            if (value != null) {
                validatePath(value, argName, allowed);
            }
        }
    }

    private void validatePath(String pathStr, String argName, List<Path> allowed) {
        if (pathStr.contains("..")) {
            throw new ToolCallException(
                    "Access denied: path traversal ('..') not permitted in argument '" + argName + "'");
        }

        Path requested = Paths.get(pathStr).toAbsolutePath().normalize();
        boolean isAllowed = allowed.stream().anyMatch(requested::startsWith);
        if (!isAllowed) {
            LOG.warnf("Blocked access to '%s' (argument '%s') — not within allowed directories: %s",
                    requested, argName, allowed);
            String suggestedDir = requested.getRoot()
                    .resolve(requested.subpath(0, Math.min(2, requested.getNameCount())))
                    .toString();
            throw new ToolCallException(
                    "Access denied: path '" + requested + "' is outside the allowed directories " + allowed + ".\n"
                            + "To grant access, the user must restart the MCP server with the directory added to the launch args.\n"
                            + "Example for the ACP --mcp-server-config:\n"
                            + "  \"args\":[\"-Dmtool.allowed-dirs=" + suggestedDir
                            + "\", \"-jar\", \"./mcp/target/mcp-1.0.7-SNAPSHOT-runner.jar\"]\n"
                            + "Tell the user to update their acp run command accordingly.");
        }
    }
}