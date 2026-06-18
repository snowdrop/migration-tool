package dev.snowdrop.mtool.model.parser;

import java.util.Map;

/**
 * The query capture the information about a condition to match
 * Example:
 * find all java.class
 * find all java.annotation
 * find all properties
 * java.annotation is '@SpringBootApplication'
 *
 * @param fileType the type of the source file (java, xml, json, properties, etc
 * @param symbol the entity we search about: class, method, property, key, etc
 * @param operation the query operation: is, find all, etc
 * @param keyValues the k=v parameters to use part of the query
 */
public record Query(String fileType, String symbol, String operation, Map<String, String> keyValues) {
}
