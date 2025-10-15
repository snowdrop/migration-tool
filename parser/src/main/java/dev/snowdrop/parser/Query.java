package dev.snowdrop.parser;

import java.util.HashMap;
import java.util.Map;

public record Query(
    String fileType,
    String symbol,
    Map<String, String> keyValues
){}
