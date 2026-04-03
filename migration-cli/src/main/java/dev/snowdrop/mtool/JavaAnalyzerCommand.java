package dev.snowdrop.mtool;

import dev.snowdrop.mtool.commands.AnalyzeCommand;
import dev.snowdrop.mtool.commands.TransformCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "mtool", description = "Quarkus mtool client able to scan, analyze and migrate a java application using instructions", subcommands = {
        AnalyzeCommand.class, TransformCommand.class,
        CommandLine.HelpCommand.class }, versionProvider = JavaAnalyzerCommand.VersionProvider.class, mixinStandardHelpOptions = true)
public class JavaAnalyzerCommand {

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String version = JavaAnalyzerCommand.class.getPackage().getImplementationVersion();
            return new String[] { "mtool " + (version != null ? version : "unknown") };
        }
    }
}