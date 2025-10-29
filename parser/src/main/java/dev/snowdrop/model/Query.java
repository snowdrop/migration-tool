package dev.snowdrop.model;

import java.util.Map;

public record Query(String fileType, String symbol, Map<String, String> keyValues) {
}
