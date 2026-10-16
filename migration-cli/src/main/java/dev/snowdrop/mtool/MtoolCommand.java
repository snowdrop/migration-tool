package dev.snowdrop.mtool;

import dev.snowdrop.mtool.commands.AnalyzeCommand;
import dev.snowdrop.mtool.commands.ScannerCommand;
import dev.snowdrop.mtool.commands.TransformCommand;
import dev.snowdrop.mtool.commands.ValidateCommand;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "mtool", description = "Quarkus mtool client able to scan, analyze, validate or migrate a java application", groupCommands = {
        AnalyzeCommand.class,
        TransformCommand.class,
        ValidateCommand.class,
        ScannerCommand.class
})
public class MtoolCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', name = "help", hasValue = false, description = "Show this help message and exit")
    private boolean help;

    @Option(shortName = 'V', name = "version", hasValue = false, description = "Print version information and exit")
    private boolean version;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (version) {
            String ver = MtoolCommand.class.getPackage().getImplementationVersion();
            commandInvocation.println("mtool " + (ver != null ? ver : "unknown"));
            return CommandResult.SUCCESS;
        }
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("mtool"));
            return CommandResult.SUCCESS;
        }
        commandInvocation.println(commandInvocation.getHelpInfo("mtool"));
        return CommandResult.SUCCESS;
    }
}
