package dev.snowdrop.analyze.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.analyze.model.Rule;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class YamlRuleParser {
    private static final Logger logger = Logger.getLogger(YamlRuleParser.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    public YamlRuleParser() {
    }

    public static Rule parseRuleFromFile(Path filePath) throws IOException {
        logger.debugf("Parsing YAML rule from file: {}", filePath);
        try {
            return yamlMapper.readValue(Files.newInputStream(filePath), Rule.class);
        } catch (IOException e) {
            logger.error("Failed to parse YAML rule from file: {}", filePath, e);
            throw e;
        }
    }

    public static List<Rule> parseRulesFromFile(Path filePath) throws IOException {
        logger.debugf("Parsing YAML rules list from file: {}", filePath);
        try {
            return yamlMapper.readValue(Files.newInputStream(filePath),
                yamlMapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
        } catch (IOException e) {
            logger.error("Failed to parse YAML rules list from file: {}", filePath, e);
            throw e;
        }
    }

    public void writeRuleToFile(Rule rule, Path filePath) throws IOException {
        logger.debugf("Writing YAML rule to file: {}", filePath);
        try {
            yamlMapper.writeValue(Files.newOutputStream(filePath), rule);
        } catch (IOException e) {
            logger.error("Failed to write YAML rule to file: {}", filePath, e);
            throw e;
        }
    }

    public String ruleToYamlString(Rule rule) throws IOException {
        logger.debugf("Converting rule to YAML string");
        try {
            return yamlMapper.writeValueAsString(rule);
        } catch (IOException e) {
            logger.error("Failed to convert rule to YAML string", e);
            throw e;
        }
    }

    public static List<Rule> parseRulesFromFolder(Path folderPath) throws IOException {
        return parseRulesFromFolder(folderPath, true);
    }

    public static  List<Rule> parseRulesFromFolder(Path folderPath, boolean recursive) throws IOException {
        logger.debugf("Parsing YAML rules from folder: {} (recursive: {})", folderPath, recursive);

        if (!Files.exists(folderPath)) {
            throw new IOException("Folder does not exist: " + folderPath);
        }

        if (!Files.isDirectory(folderPath)) {
            throw new IOException("Path is not a directory: " + folderPath);
        }

        List<Rule> rules = new ArrayList<>();

        if (recursive) {
            parseRulesRecursively(folderPath, rules);
        } else {
            parseRulesFromDirectFolder(folderPath, rules);
        }

        logger.debugf("Parsed {} rules from folder: {}", rules.size(), folderPath);
        return rules;
    }

    private static void parseRulesRecursively(Path folderPath, List<Rule> rules) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    parseRulesRecursively(entry, rules);
                } else if (isYamlFile(entry)) {
                    try {
                        List<Rule> fileRules = parseRulesFromFile(entry);
                        rules.addAll(fileRules);
                        logger.debugf("Successfully parsed {} rules from: {}", fileRules.size(), entry);
                    } catch (IOException e) {
                        logger.warnf("Failed to parse rules from file: {} - {}", entry, e.getMessage());
                    }
                }
            }
        }
    }

    private static void parseRulesFromDirectFolder(Path folderPath, List<Rule> rules) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath, YamlRuleParser::isYamlFile)) {
            for (Path yamlFile : stream) {
                try {
                    List<Rule> fileRules = parseRulesFromFile(yamlFile);
                    rules.addAll(fileRules);
                    logger.debugf("Successfully parsed {} rules from: {}", fileRules.size(), yamlFile);
                } catch (IOException e) {
                    logger.warnf("Failed to parse rule from file: {} - {}", yamlFile, e.getMessage());
                }
            }
        }
    }

    private static boolean isYamlFile(Path path) {
        if (Files.isDirectory(path)) {
            return false;
        }
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }
}