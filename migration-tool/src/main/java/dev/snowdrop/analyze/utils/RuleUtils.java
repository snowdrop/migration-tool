package dev.snowdrop.analyze.utils;

import dev.snowdrop.analyze.model.Rule;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class RuleUtils {
    private static final Logger logger = Logger.getLogger(RuleUtils.class);

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
                logger.warnf("Unknown location type '{}', defaulting to 0", location);
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

    public static List<Rule> loadRules(Path rulesPath) {
        logger.info("📋 Loading migration rules...");

        try {
            File rulesDir = new File(rulesPath.toUri());
            if (!rulesDir.exists()) {
                logger.errorf("⚠️  Rules directory not found: %s", rulesPath);
                return List.of();
            }

            return YamlRuleParser.parseRulesFromFolder(rulesDir.toPath());
        } catch (Exception e) {
            logger.errorf("❌ Error loading rules: %s", e.getMessage());
            return List.of();
        }
    }

    /**
     * Add a method to get the value of the SymbolKind
     * File(1),
	 * Module(2),
	 * Namespace(3),
	 * Package(4),
	 * Class(5),
	 * Method(6),
	 * Property(7),
	 * Field(8),
	 * Constructor(9),
	 * Enum(10),
	 * Interface(11),
	 * Function(12),
	 * Variable(13),
	 * Constant(14),
	 * String(15),
	 * Number(16),
	 * Boolean(17),
	 * Array(18),
	 * Object(19),
	 * Key(20),
	 * Null(21),
	 * EnumMember(22),
	 * Struct(23),
	 * Event(24),
	 * Operator(25),
	 * TypeParameter(26);
     */
}
