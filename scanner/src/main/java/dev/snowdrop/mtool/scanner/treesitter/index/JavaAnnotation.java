package dev.snowdrop.mtool.scanner.treesitter.index;

import java.util.Map;

public record JavaAnnotation(
        String name,
        Map<String, String> arguments,
        int startRow,
        int startColumn,
        int endRow,
        int endColumn) {

    public boolean hasArgument(String key, String value) {
        return value.equals(arguments.get(key));
    }

    public String getArgument(String key) {
        return arguments.get(key);
    }

    public boolean isMarker() {
        return arguments.isEmpty();
    }
}
