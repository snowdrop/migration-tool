package dev.snowdrop.analyze.utils;

import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.Styler;

import java.util.List;
import java.util.stream.Collectors;

public class TerminalUtils {

	private static final String ESC_CHAR = "\u001B"; // ESCAPE character
	private static final String ST = "\u001B\\"; // String Terminator
	private static final String HYPERLINK_CMD = "]8;;"; // OS command to create a hyperlink

	public static final String GREEN = "\u001B[32m";
	public static final String RESET = "\u001B[0m";

	/**
	 * Creates a Styler that adds hyperlinks to the first column and colors headers green.
	 *
	 * @param ruleRepoUrlFormat
	 *            the URL format string for rules (e.g., "https://example.com/rule/%s")
	 *
	 * @return a configured Styler instance
	 */
	public static Styler customizeStyle(String ruleRepoUrlFormat) {
		return new Styler() {
			@Override
			public List<String> styleCell(Column column, int row, int col, List<String> data) {
				if (col != 0) {
					return data;
				}
				return data.stream().map(line -> createLink(String.format(ruleRepoUrlFormat, line.trim()), line))
						.collect(Collectors.toList());
			}

			@Override
			public List<String> styleHeader(Column column, int col, List<String> data) {
				return data.stream().map(line -> colorize(line, GREEN)).collect(Collectors.toList());
			}
		};
	}

	/**
	 * Applies color formatting to text.
	 *
	 * @param text
	 *            the text to color
	 * @param color
	 *            the ANSI color code
	 *
	 * @return the colored text
	 */
	public static String colorize(String text, String color) {
		return color + text + RESET;
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

}
