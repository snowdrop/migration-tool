package dev.snowdrop.mtool;

import dev.snowdrop.mtool.commands.AnalyzeCommand;
import dev.snowdrop.mtool.commands.TransformCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "mtool", description = "Quarkus mtool client able to scan, analyze and migrate a java application using instructions", subcommands = {
		AnalyzeCommand.class, TransformCommand.class, CommandLine.HelpCommand.class})
public class JavaAnalyzerCommand {
}