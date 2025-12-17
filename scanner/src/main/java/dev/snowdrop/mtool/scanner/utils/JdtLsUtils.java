package dev.snowdrop.mtool.scanner.utils;

import org.jboss.logging.Logger;

public class JdtLsUtils {
	private static final Logger logger = Logger.getLogger(JdtLsUtils.class);

	public static String getLocationCode(String location) {
		if (location == null) {
			return "0";
		}

		return switch (location.toUpperCase()) {
			case "" -> "0";
			case "INHERITANCE" -> "1";
			case "METHOD_CALL" -> "2";
			case "CONSTRUCTOR_CALL" -> "3";
			case "ANNOTATION" -> "4";
			case "IMPLEMENTS_TYPE" -> "5";
			case "ENUM" -> "6";
			case "RETURN_TYPE" -> "7";
			case "IMPORT" -> "8";
			case "VARIABLE_DECLARATION" -> "9";
			case "TYPE" -> "10";
			case "PACKAGE" -> "11";
			case "FIELD" -> "12";
			case "METHOD" -> "13";
			case "CLASS" -> "14";
			default -> {
				logger.errorf("Unknown language server location type %s, defaulting to 0", location);
				yield "0";
			}
		};
	}

	public static String getLocationName(String code) {
		if (code == null) {
			return "UNKNOWN";
		}

		return switch (code) {
			case "0" -> "UNKNOWN";
			case "1" -> "INHERITANCE";
			case "2" -> "METHOD_CALL";
			case "3" -> "CONSTRUCTOR_CALL";
			case "4" -> "ANNOTATION";
			case "5" -> "IMPLEMENTS_TYPE";
			case "6" -> "ENUM";
			case "7" -> "RETURN_TYPE";
			case "8" -> "IMPORT";
			case "9" -> "VARIABLE_DECLARATION";
			case "10" -> "TYPE";
			case "11" -> "PACKAGE";
			case "12" -> "FIELD";
			case "13" -> "METHOD";
			case "14" -> "CLASS";
			default -> {
				logger.warnf("Unknown location code '{}', defaulting to UNKNOWN", code);
				yield "UNKNOWN";
			}
		};
	}
}
