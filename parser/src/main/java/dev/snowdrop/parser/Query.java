package dev.snowdrop.parser;

import java.util.HashMap;
import java.util.Map;

public class Query {
    private String fileType;
    private String symbol;
    private Map<String, String> keyValues = new HashMap<>();

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Map<String, String> getKeyValues() { return keyValues; }
    public void setKeyValues(Map<String, String> keyValues) { this.keyValues = keyValues; }
}
