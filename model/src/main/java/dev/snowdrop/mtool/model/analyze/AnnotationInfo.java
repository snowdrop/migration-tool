package dev.snowdrop.mtool.model.analyze;

import java.util.Map;

public record AnnotationInfo(
        String name,
        Map<String, String> arguments,
        String filePath,
        int line,
        int column) {

    public boolean hasArgument(String key, String value) {
        return value.equals(arguments.get(key));
    }

    public String getArgument(String key) {
        return arguments.get(key);
    }
}
