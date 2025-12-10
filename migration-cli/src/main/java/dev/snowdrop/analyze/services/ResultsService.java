package dev.snowdrop.analyze.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import com.github.freva.asciitable.Styler;
import dev.snowdrop.analyze.model.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.html.Cell;
import dev.snowdrop.analyze.model.html.Row;
import dev.snowdrop.analyze.utils.TerminalUtils;
import dev.snowdrop.transform.model.MigrationTasksExport;
import io.quarkus.qute.*;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResultsService {
	private static final Logger logger = Logger.getLogger(ResultsService.class);

	public static final String RULE_REPO_URL_FORMAT;

	static {
		RULE_REPO_URL_FORMAT = ConfigProvider.getConfig().getValue("analyzer.rules.repo_url", String.class);
	}

	private final String source;
	private final String target;

	public ResultsService(String source, String target) {
		this.source = source;
		this.target = target;
	}

	// This regex finds URLs starting with http://, https://, or file:///
	// and captures them. It stops at whitespace or a '<' (to avoid our <br> tag).
	private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s<]+|file:///[^\\s<]+)");

	public void exportAsHtml(Config config, List<String[]> rawTableData) {
		String[] headers = {"Rule ID", "Source to Target", "Match", "Information Details"};
		List<Row> tableData = convertToRows(headers, rawTableData);

		TemplateLocator templateLocator = getTemplateLocator(".html");
		Engine engine = Engine.builder().addLocator(templateLocator).addDefaults()
				.addValueResolver(new ReflectionValueResolver()).build();

		try {
			Template reportTmpl = engine.getTemplate("report");
			if (reportTmpl == null) {
				logger.error("Could not load template: report");
				return;
			}

			String report = reportTmpl.data("tableData", tableData).data("headers", headers).render();
			logger.debugf(report);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
					.withLocale(Locale.getDefault());
			String dateTimeformated = LocalDateTime.now().format(formatter);

			String reportHtmlFileName = String.format("%s/analysing-%s-report_%s.html", config.appPath(),
					config.scanner(), dateTimeformated);
			Files.writeString(Paths.get(reportHtmlFileName), report);

			logger.infof("==== HTML Report file exported to: file:///%s", reportHtmlFileName);
		} catch (Exception e) {
			logger.error("Error rendering template: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void exportAsCsv(Config config, List<String[]> rawTableData) {
		String[] headers = {"Rule ID", "Source to Target", "Match", "Information Details"};
		List<Row> tableData = convertToRows(headers, rawTableData);

		TemplateLocator templateLocator = getTemplateLocator(".csv");
		Engine engine = Engine.builder().addLocator(templateLocator).addDefaults()
				.addValueResolver(new ReflectionValueResolver()).build();

		try {
			Template reportTmpl = engine.getTemplate("report");
			if (reportTmpl == null) {
				logger.error("Could not load template: report");
				return;
			}

			String report = reportTmpl.data("tableData", tableData).data("headers", headers).render();
			logger.debugf(report);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
					.withLocale(Locale.getDefault());
			String dateTimeformated = LocalDateTime.now().format(formatter);

			String reportHtmlFileName = String.format("%s/analysing-%s-report_%s.csv", config.appPath(),
					config.scanner(), dateTimeformated);
			Files.writeString(Paths.get(reportHtmlFileName), report);

			logger.infof("==== CSV Report file exported to: file:///%s", reportHtmlFileName);
		} catch (Exception e) {
			logger.error("Error rendering template: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void showCsvTable(List<String[]> tableData) {
		System.out.println("\n=== Code Analysis Results ===");
		String asciiTable = AsciiTable.builder().styler(TerminalUtils.customizeStyle(RULE_REPO_URL_FORMAT))
				.data(tableData,
						Arrays.asList(
								new Column().header("Rule ID").headerAlign(HorizontalAlign.LEFT)
										.dataAlign(HorizontalAlign.LEFT).with(r -> r[0]),
								new Column().header("Source to Target").headerAlign(HorizontalAlign.LEFT)
										.dataAlign(HorizontalAlign.LEFT).with(r -> r[1]),
								new Column().header("Match").headerAlign(HorizontalAlign.CENTER)
										.dataAlign(HorizontalAlign.CENTER).with(r -> r[2]),
								new Column().header("Information Details").headerAlign(HorizontalAlign.LEFT)
										.maxWidth(120).dataAlign(HorizontalAlign.LEFT).with(r -> r[3])))
				.asString();
		System.out.println(asciiTable);
	}

	/**
	 * Helper function to create an OSC 8 terminal hyperlink.
	 * <p>
	 * ESC ]8;\nLINK ST TEXT ESC ]8;\nST
	 * <p>
	 * ESC is the ESCAPE character ST is the String terminator
	 * <p>
	 * Example of commands printf '\u001B]8;;https://google.com\u001B\\Click Me\u001B]8;;\u001B\\ \n'
	 */
	public static String createLink(String url, String text) {
		String ESC_CHAR = "\u001B"; // ESCAPE character
		String ST = "\u001B\\"; // String Terminator
		String HYPERLINK_CMD = "]8;;"; // OS command to crate a hyperlink
		String OSC8_START = ESC_CHAR + HYPERLINK_CMD + url + ST;
		String OSC8_END = ESC_CHAR + HYPERLINK_CMD + ST;
		return OSC8_START + text + OSC8_END;
	}

	public static Styler customizeStyle() {
		var RULE_REPO_URL = ConfigProvider.getConfig().getValue("analyzer.rules.repo_url", String.class);
		String RESET = "\u001B[m";
		String GREEN = "\u001B[32m";

		return new Styler() {
			@Override
			public List<String> styleCell(Column column, int row, int col, List<String> data) {
				if (col != 0) {
					return data;
				}
				return data.stream().map(line -> {
					if (col == 0) {
						return createLink(String.format(RULE_REPO_URL, line.trim()), line);
					} else {
						return line;
					}
				}).collect(Collectors.toList());
			}

			@Override
			public List<String> styleHeader(Column column, int col, List<String> data) {
				return data.stream().map(line -> GREEN + line + RESET).collect(Collectors.toList());
			}
		};
	}

	/**
	 * Converts a raw List of String arrays into a structured List of Rows. * @param headers The header array (e.g.,
	 * ["RuleID", "Match", ...])
	 *
	 * @param tableData
	 *            The raw data rows (List<String[]>)
	 *
	 * @return A List<Row> ready for the Qute template
	 */
	private static List<Row> convertToRows(String[] headers, List<String[]> tableData) {

		boolean isRuleIdColumn = headers.length > 0 && "Rule ID".equalsIgnoreCase(headers[0]);
		List<Row> resultRows = new ArrayList<>();

		for (String[] stringRow : tableData) {
			List<Cell> cells = new ArrayList<>();

			for (int i = 0; i < stringRow.length; i++) {
				String rawCellText = stringRow[i];

				// Handle the "RuleID" column
				if (i == 0 && isRuleIdColumn && rawCellText != null && !rawCellText.isBlank()) {
					String url = String.format(RULE_REPO_URL_FORMAT, rawCellText);
					String displayText = rawCellText.replace("\n", "<br>");

					cells.add(new Cell(displayText, url));
				} else {
					if (rawCellText == null) {
						cells.add(new Cell(""));
						continue;
					}

					// Replace newlines
					String displayText = rawCellText.replace("\n", "<br>");

					// Find and replace all embedded URIs
					String replacement = "<a href=\"$0\">$0</a>";
					displayText = URL_PATTERN.matcher(displayText).replaceAll(replacement);

					cells.add(new Cell(displayText));
				}
			}
			resultRows.add(new Row(cells));
		}

		return resultRows;
	}

	private static @NotNull TemplateLocator getTemplateLocator(String suffix) {
		final String templateBasePath = "/templates/";

		TemplateLocator templateLocator = new TemplateLocator() {
			@Override
			public Optional<TemplateLocation> locate(String id) {
				String fullPath = templateBasePath + id + suffix;
				InputStream in = ResultsService.class.getResourceAsStream(fullPath);

				if (in == null) {
					System.err.println("Template not found: " + fullPath);
					return Optional.empty();
				}

				// Create a TemplateLocation that can read from the InputStream
				return Optional.of(new TemplateLocation() {
					@Override
					public Reader read() {
						// Get a fresh InputStream for each read
						InputStream inputStream = ResultsService.class.getResourceAsStream(fullPath);
						if (inputStream == null) {
							throw new RuntimeException("Template not found: " + fullPath);
						}
						return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
					}

					@Override
					public Optional<Variant> getVariant() {
						return Optional.empty();
					}
				});
			}
		};
		return templateLocator;
	}

	public List<String[]> generateDataTable(Map<String, MigrationTask> results, String source, String target) {
		// Prepare data for the table
		List<String[]> tableData = new ArrayList<>();

		for (Map.Entry<String, MigrationTask> entry : results.entrySet()) {
			String ruleId = entry.getKey();
			MigrationTask aTask = entry.getValue();

			List<Match> queryResults = new ArrayList<>();

			/* This code is deprecated since we handle now a query/scanner (and not all queries for one scanner only) and use a match result
			if (aTask.getLsResults() != null && !aTask.getLsResults().isEmpty()) {
				queryResults = aTask.getLsResults();
			}

			if (aTask.getRewriteResults() != null && !aTask.getRewriteResults().isEmpty()) {
				queryResults = aTask.getRewriteResults();
			}
			*/

			if (aTask.getMatchResults() != null && !aTask.getMatchResults().isEmpty()) {
				queryResults = aTask.getMatchResults();
			}

			String hasQueryResults = queryResults.isEmpty() ? "No" : "Yes";
			String sourceToTarget = String.format("%s -> %s", source, target);

			if (queryResults.isEmpty()) {
				tableData.add(new String[]{ruleId, sourceToTarget, hasQueryResults, "No match found"});
			} else {
				StringBuilder allResultsDetails = new StringBuilder();

				for (int i = 0; i < queryResults.size(); i++) {
					Match match = queryResults.get(i);

					String resultDetails = formatMatchResult(match);
					if (!resultDetails.isEmpty()) {
						allResultsDetails.append(resultDetails);
					} else {
						allResultsDetails.append("No results found");
					}

					/* This code is deprecated since we handle now a query/scanner (and not all queries for one scanner only) and use a match result
					if (result instanceof SymbolInformation symbolInfo) {
						String symbolDetails = formatSymbolInformation(symbolInfo);
						allResultsDetails.append(symbolDetails).append("\n").append(symbolInfo.getLocation().getUri());
					} else if (result instanceof Match match) {
						String resultDetails = formatRewriteImproved(match);
						allResultsDetails.append(resultDetails);
					} else {
						// Fallback for unknown types
						allResultsDetails.append("Unknown result type: ").append(result.getClass().getSimpleName());
					}
					*/

					// Add separator between multiple results (except for the last one)
					if (i < queryResults.size() - 1) {
						allResultsDetails.append("\n--- result ---\n");
					}
				}

				tableData.add(new String[]{ruleId, sourceToTarget, hasQueryResults, allResultsDetails.toString()});
			}
		}

		// Sorts the List<String[]> by comparing the first element (row[0]) of each array which is the RuleID
		tableData.sort(Comparator.comparing(row -> row[0]));

		return tableData;
	}

	private static String formatMatchResult(Match match) {

		switch (match.scannerType()) {
			case "jdtls" :
				var symbolInformations = (ArrayList<SymbolInformation>) match.result();
				// TODO: Should we consider to support to handle a list of SymbolInformation when we issue a jdtls query too ?
				var symbolDetails = formatSymbolInformation(symbolInformations.get(0));
				symbolDetails.append("\n").append(symbolInformations.get(0).getLocation().getUri());
				return symbolDetails.toString();
			case "openrewrite" :
				return formatRewrite(match);
			case "maven", "file-search" :
				return match.result().toString();
			default :
				return "";
		}
	}

	private static String formatRewrite(Match match) {
		String name = (String) match.result();

		// Parse the name format: parentFolderName/csvFileName:line_number|pattern.symbol|type
		if (name.contains("/") && name.contains(":") && name.contains("|")) {
			try {
				String[] pathAndRest = name.split(":", 2);
				String path = pathAndRest[0];
				String[] lineAndDetails = pathAndRest[1].split("\\|", 3);
				String lineNumber = lineAndDetails[0];
				String patternSymbol = lineAndDetails.length > 1 ? lineAndDetails[1] : "N/A";
				String type = lineAndDetails.length > 2 ? lineAndDetails[2] : "N/A";

				return String.format("File: %s\nLine: %s\nPattern: %s\nType: %s\nMatch ID: %s", path, lineNumber,
						patternSymbol, type, match.matchId());
			} catch (Exception e) {
				// Fallback if parsing fails
				return String.format("Rewrite match: %s (ID: %s)", (String) match.result(), match.matchId());
			}
		} else {
			// Fallback for unexpected format
			return String.format("Rewrite match: %s (ID: %s)", (String) match.result(), match.matchId());
		}
	}

	private static StringBuilder formatSymbolInformation(SymbolInformation si) {
		var formatStr = String.format("Found %s at line %s, char: %s - %s", si.getName(),
				si.getLocation().getRange().getStart().getLine() + 1,
				si.getLocation().getRange().getStart().getCharacter(),
				si.getLocation().getRange().getEnd().getCharacter());
		return new StringBuilder(formatStr);
	}

	public void exportAsJson(Config config, Map<String, MigrationTask> tasks) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
					.withLocale(Locale.getDefault());
			String dateTimeformated = LocalDateTime.now().format(formatter);

			MigrationTasksExport exportData = new MigrationTasksExport("Migration Analysis Results", config.appPath(),
					dateTimeformated, tasks);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
			File outputFile = new File(String.format("%s/analysing-%s-report_%s.json", config.appPath(),
					config.scanner(), dateTimeformated));

			// Ensure parent directory exists
			if (outputFile.getParentFile() != null) {
				outputFile.getParentFile().mkdirs();
			}

			objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, exportData);
			logger.infof("📄 Migration tasks exported to: %s", outputFile);

		} catch (IOException e) {
			logger.errorf("❌ Failed to export migration tasks to JSON: %s", e.getMessage());
			if (config.verbose()) {
				logger.error("Export error details:", e);
			}
		}
	}
}