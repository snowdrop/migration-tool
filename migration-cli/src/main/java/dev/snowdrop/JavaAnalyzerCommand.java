package dev.snowdrop;

import dev.snowdrop.commands.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "hal", description = "Quarkus Hal client able to scan, analyze and migrate a java application using instructions", subcommands = {
        AnalyzeCommand.class, TransformCommand.class, CommandLine.HelpCommand.class })
public class JavaAnalyzerCommand {
}