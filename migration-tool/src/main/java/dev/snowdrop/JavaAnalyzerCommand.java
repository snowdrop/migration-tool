package dev.snowdrop;

import dev.snowdrop.commands.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "quarkus-analyzer",
    description = "Quarkus CLI Analyzer",
    subcommands = {
        AnalyzeCommand.class,
        TransformCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class JavaAnalyzerCommand {
}