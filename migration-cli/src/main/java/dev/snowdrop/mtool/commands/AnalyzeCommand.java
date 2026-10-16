package dev.snowdrop.mtool.commands;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.MigrationTask;
import dev.snowdrop.mtool.model.analyze.Rule;
import dev.snowdrop.mtool.analyze.AnalyzeService;
import dev.snowdrop.mtool.analyze.reporting.ResultsService;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.snowdrop.mtool.scanner.utils.FileUtils.resolvePath;
import static dev.snowdrop.mtool.parser.rule.YamlRuleParser.filterRules;
import static dev.snowdrop.mtool.parser.rule.YamlRuleParser.parseRulesFromFile;
import static dev.snowdrop.mtool.parser.rule.YamlRuleParser.parseRulesFromFolder;

@CommandDefinition(name = "analyze", description = "Analyze a project for migration")
public class AnalyzeCommand implements Command<CommandInvocation> {
    private static final Logger logger = Logger.getLogger(AnalyzeCommand.class);

    @Argument(description = "Path to the Java project to analyze", required = true)
    public String appPath;

    @Option(shortName = 'r', name = "rules", description = "Path to rules directory (default: from config)")
    public String rulesPath;

    @Option(shortName = 's', name = "source", description = "Source technology to consider for analysis")
    public String source;

    @Option(shortName = 't', name = "target", description = "Target technology to consider for analysis")
    public String target;

    @Option(shortName = 'v', name = "verbose", hasValue = false, description = "Enable verbose output")
    public boolean verbose;

    @Option(shortName = 'o', name = "output", description = "Export the analysing result using as format: json (default), csv, html")
    private String output;

    @Option(name = "scanner", description = "Scanner tool to be used to analyse the code: openrewrite (default)", defaultValue = {
            "openrewrite" })
    public String scanner;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        Config config = fromCommandOrElseProperties();
        try {
            List<Rule> rules = loadRules(config.rulesPath(), config.sourceTechnology(), config.targetTechnology());

            AnalyzeService analyzeService = new AnalyzeService(config);
            Map<String, MigrationTask> tasks = analyzeService.analyze(rules);

            displayResults(tasks, config);

        } catch (Exception e) {
            logger.errorf("Error: %s", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
        return CommandResult.SUCCESS;
    }

    public Config fromCommandOrElseProperties() {
        Path path = Paths.get(appPath);
        if (!path.toFile().exists()) {
            logger.errorf("❌ Project path of the application does not exist: %s", appPath);
            throw new IllegalStateException("❌ Project path of the application does not exist: " + appPath);
        }

        String appPathString = resolvePath(appPath).toString();

        String rulesPathString = Optional.ofNullable(rulesPath)
                .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.rules-path", String.class)))
                .orElseThrow(() -> new RuntimeException("Rules path is required but not configured"));
        Path resolvedRulesPath = resolvePath(rulesPathString);

        String sourceTechnology = Optional.ofNullable(source)
                .or(() -> Optional
                        .ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-source", String.class)))
                .orElseThrow(() -> new RuntimeException("Source technology to analyse required but not configured"));

        String targetTechnology = Optional.ofNullable(target)
                .or(() -> Optional
                        .ofNullable(ConfigProvider.getConfig().getValue("analyzer.technology-target", String.class)))
                .orElseThrow(
                        () -> new RuntimeException("Target technology for migration is required but not configured"));

        String openRewriteMavenPluginVersion = Optional
                .ofNullable(ConfigProvider.getConfig().getValue("openrewrite.maven-plugin.version", String.class))
                .orElseThrow(() -> new RuntimeException("Openrewrite maven plugin version not define"));

        Config config = new Config(appPathString, resolvedRulesPath, sourceTechnology, targetTechnology,
                verbose, output, scanner, openRewriteMavenPluginVersion);

        logger.infof("Application path: %s", appPath);
        logger.infof("Source technology: %s", sourceTechnology);
        logger.infof("Target technology: %s", targetTechnology);
        logger.infof("Openrewrite maven plugin version: %s", openRewriteMavenPluginVersion);
        return config;
    }

    public List<Rule> loadRules(Path rulesPath, String sourceTech, String targetTech) throws IOException {

        List<Rule> rules;
        if (Files.isRegularFile(rulesPath)) {
            rules = parseRulesFromFile(rulesPath).stream().sorted(Comparator.comparingInt(Rule::order)).toList();
        } else {
            rules = parseRulesFromFolder(rulesPath);
        }

        List<Rule> filteredRules = filterRules(rules, sourceTech, targetTech);
        if (filteredRules.isEmpty()) {
            logger.warnf("No rules found !!");
        }
        return filteredRules;
    }

    private void displayResults(Map<String, MigrationTask> tasks, Config config) throws InterruptedException {
        ResultsService resultsService = new ResultsService(config.sourceTechnology(), config.targetTechnology());
        if (tasks.isEmpty()) {
            logger.warnf("No migration tasks found !!");
        }

        List<String[]> tableData = resultsService.generateDataTable(tasks, config.sourceTechnology(),
                config.targetTechnology());
        resultsService.showCsvTable(tableData);

        resultsService.exportAsJson(config, tasks);

        if (config.output() != null && !config.output().isEmpty()) {
            switch (output) {
                case "html" -> resultsService.exportAsHtml(config, tableData);
                case "csv" -> resultsService.exportAsCsv(config, tableData);
                default -> logger.warnf("The format selected to export the report is unknown: %s", output);
            }
        }

        logger.infof("Waiting for commands to complete...");
        Thread.sleep(2000);
    }

}
