package dev.snowdrop.mtool.model.parser;

import java.util.Map;

public record Query(String fileType, String symbol, Map<String, String> keyValues) {
}
