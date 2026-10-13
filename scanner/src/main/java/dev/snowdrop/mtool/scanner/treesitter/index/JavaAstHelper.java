package dev.snowdrop.mtool.scanner.treesitter.index;

import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterNode;
import io.roastedroot.treesitter.TreeSitterQuery;
import io.roastedroot.treesitter.TreeSitterQueryResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaAstHelper {

    private static final String ANNOTATION_QUERY = """
            (marker_annotation) @annotation
            (annotation) @annotation
            """;

    private static final String IMPORT_QUERY = """
            (import_declaration (scoped_identifier) @import_path)
            """;

    private JavaAstHelper() {
    }

    static List<JavaAnnotation> getAnnotations(TreeSitter ts, TreeSitterNode rootNode, String source) {
        List<JavaAnnotation> annotations = new ArrayList<>();
        try (TreeSitterQuery query = ts.newQuery(Language.JAVA, ANNOTATION_QUERY)) {
            List<TreeSitterQueryResult> results = query.exec(rootNode, source);
            for (TreeSitterQueryResult result : results) {
                annotations.add(parseAnnotation(result.node(), source));
            }
        }
        return annotations;
    }

    static JavaAnnotation parseAnnotation(TreeSitterNode node, String source) {
        String name = "";
        Map<String, String> arguments = new LinkedHashMap<>();

        for (int i = 0; i < node.namedChildCount(); i++) {
            TreeSitterNode child = node.namedChild(i);
            if ("identifier".equals(child.type())) {
                name = child.text(source);
            } else if ("annotation_argument_list".equals(child.type())) {
                extractArguments(child, source, arguments);
            }
        }

        return new JavaAnnotation(
                name,
                Collections.unmodifiableMap(arguments),
                node.startRow(), node.startColumn(),
                node.endRow(), node.endColumn());
    }

    static Map<String, String> getImportMap(TreeSitter ts, TreeSitterNode rootNode, String source) {
        Map<String, String> importMap = new LinkedHashMap<>();
        try (TreeSitterQuery query = ts.newQuery(Language.JAVA, IMPORT_QUERY)) {
            List<TreeSitterQueryResult> results = query.exec(rootNode, source);
            for (TreeSitterQueryResult result : results) {
                String fqn = result.node().text(source);
                String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
                importMap.put(simpleName, fqn);
            }
        }
        return importMap;
    }

    static String resolveAnnotationFqn(JavaAnnotation annotation, Map<String, String> importMap) {
        return importMap.getOrDefault(annotation.name(), annotation.name());
    }

    private static void extractArguments(TreeSitterNode argListNode, String source, Map<String, String> arguments) {
        for (int i = 0; i < argListNode.namedChildCount(); i++) {
            TreeSitterNode child = argListNode.namedChild(i);
            if ("element_value_pair".equals(child.type()) && child.namedChildCount() >= 2) {
                String key = child.namedChild(0).text(source);
                String value = child.namedChild(1).text(source);
                arguments.put(key, value);
            } else if (!"element_value_pair".equals(child.type())) {
                arguments.put("value", child.text(source));
            }
        }
    }
}
