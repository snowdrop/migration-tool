package dev.snowdrop.mtool.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

import java.text.SimpleDateFormat;

public class AnalyseTools {

    @Tool(description = "Analyze the source to discover the java classes, properties, etc files")
    public String sourceAnalyze(@ToolArg(description = "Project path", defaultValue = ".") String projectPath) {
        return "Analyzing the source: " + projectPath;
    }
}
