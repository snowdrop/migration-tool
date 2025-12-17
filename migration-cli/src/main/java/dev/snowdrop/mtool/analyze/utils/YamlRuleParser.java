package dev.snowdrop.mtool.analyze.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.mtool.model.analyze.Rule;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class YamlRuleParser {
	private static final Logger logger = Logger.getLogger(YamlRuleParser.class);

	private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

	public YamlRuleParser() {
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

	public static List<Rule> filterRules(List<Rule> rules, String source, String target) {
		List<Rule> filteredRules = new ArrayList<>();
		String sourceLabel = "konveyor.io/source=" + source;
		String targetLabel = "konveyor.io/target=" + target;

		// The two labels we need to search about
		List<String> requiredLabels = List.of(sourceLabel, targetLabel);

		rules.forEach(rule -> {
			if (rule.labels().containsAll(requiredLabels)) {
				filteredRules.add(rule);
			} else {
				logger.warnf("RuleID %s with labels %s is missing one or both label: from %s -> %s", rule.ruleID(),
						rule.labels(), sourceLabel, targetLabel);
			}
		});
		return filteredRules;
	}

	public static List<Rule> parseRulesFromFolder(Path folderPath) throws IOException {
		return parseRulesFromFolder(folderPath, true);
	}

	public static List<Rule> parseRulesFromFolder(Path folderPath, boolean recursive) throws IOException {
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

		List<Rule> sortedRules = rules.stream().sorted(Comparator.comparingInt(Rule::order)).toList();

		logger.debugf("Parsed {} rules from folder: {}", sortedRules.size(), folderPath);
		return sortedRules;
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