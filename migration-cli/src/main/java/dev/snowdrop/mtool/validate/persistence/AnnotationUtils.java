package dev.snowdrop.mtool.validate.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing JPA annotation strings extracted by TreeSitter.
 * Adapted from IBM's quarkus-skills AnnotationUtils.
 */
class AnnotationUtils {

    static String extractAnnotationField(String annotation, String key) {
        if (annotation == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                key + "\\s*=\\s*\"?([^\"]+?)\"?(,|\\))", Pattern.DOTALL);
        Matcher m = pattern.matcher(annotation);
        if (!m.find()) {
            return null;
        }

        String value = m.group(1).trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    static boolean isEntityAnnotation(String ann) {
        String name = getAnnotationName(ann);
        return "Entity".equals(name);
    }

    static String getAnnotationName(String annotationStr) {
        if (annotationStr == null) {
            return null;
        }
        Pattern p = Pattern.compile("@(?:[\\w.]+\\.)?([\\w]+)");
        Matcher m = p.matcher(annotationStr);
        return m.find() ? m.group(1) : null;
    }

    static String[] normalizeType(String typeStr) {
        if (typeStr != null && typeStr.contains("<") && typeStr.contains(">")) {
            int lt = typeStr.indexOf('<');
            int gt = typeStr.indexOf('>');
            String collection = typeStr.substring(0, lt);
            if (collection.contains(".")) {
                collection = collection.substring(collection.lastIndexOf('.') + 1);
            }
            String inner = typeStr.substring(lt + 1, gt);
            return new String[] { collection, inner };
        }
        return new String[] { null, typeStr };
    }

    static List<String> parseCascade(String annotation) {
        String cascade = extractAnnotationField(annotation, "cascade");
        if (cascade == null) {
            return null;
        }
        cascade = cascade.replace("{", "").replace("}", "");
        List<String> result = new ArrayList<>();
        for (String c : cascade.split(",")) {
            String trimmed = c.trim();
            int dot = trimmed.lastIndexOf('.');
            result.add(dot >= 0 ? trimmed.substring(dot + 1) : trimmed);
        }
        return result;
    }

    static String simpleName(String s) {
        if (s == null) {
            return null;
        }
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
    }
}