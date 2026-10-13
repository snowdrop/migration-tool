package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;
import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaParameter;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterNode;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaTypeExtractor {

    private JavaTypeExtractor() {
    }

    public static List<JavaTypeDeclaration> extract(TreeSitter ts, JavaSource source) {
        try (TreeSitterParser parser = ts.newParser(Language.JAVA)) {
            return extract(ts, parser, source);
        }
    }

    public static List<JavaTypeDeclaration> extract(TreeSitter ts, TreeSitterParser parser, JavaSource source) {
        List<JavaTypeDeclaration> declarations = new ArrayList<>();

        try (TreeSitterTree tree = parser.parseString(source.sourceCode())) {

            TreeSitterNode root = tree.rootNode();
            String src = source.sourceCode();

            String packageName = extractPackageName(root, src);
            Map<String, String> importMap = JavaAstHelper.getImportMap(ts, root, src);

            for (int i = 0; i < root.namedChildCount(); i++) {
                TreeSitterNode child = root.namedChild(i);
                JavaTypeKind kind = nodeTypeToKind(child.type());
                if (kind != null) {
                    declarations.add(extractTypeDeclaration(child, src, packageName, importMap, kind, source.path()));
                }
            }
        }

        return declarations;
    }

    private static JavaTypeKind nodeTypeToKind(String nodeType) {
        return switch (nodeType) {
            case "class_declaration" -> JavaTypeKind.CLASS;
            case "interface_declaration" -> JavaTypeKind.INTERFACE;
            case "enum_declaration" -> JavaTypeKind.ENUM;
            case "record_declaration" -> JavaTypeKind.RECORD;
            case "annotation_type_declaration" -> JavaTypeKind.ANNOTATION_TYPE;
            default -> null;
        };
    }

    private static String extractPackageName(TreeSitterNode root, String source) {
        TreeSitterNode pkgDecl = findNamedChild(root, "package_declaration");
        if (pkgDecl == null) {
            return "";
        }
        TreeSitterNode scopedId = findNamedChild(pkgDecl, "scoped_identifier");
        if (scopedId != null) {
            return scopedId.text(source);
        }
        TreeSitterNode id = findNamedChild(pkgDecl, "identifier");
        return id != null ? id.text(source) : "";
    }

    private static JavaTypeDeclaration extractTypeDeclaration(TreeSitterNode node, String source,
            String packageName, Map<String, String> importMap, JavaTypeKind kind, String sourcePath) {

        String simpleName = extractIdentifier(node, source);
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        Set<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        List<AnnotationInfo> annotations = new ArrayList<>();
        extractModifiersAndAnnotations(node, source, modifiers, annotations);

        String superclass = extractSuperclass(node, source);
        List<String> interfaces = extractInterfaces(node, source, kind);

        TreeSitterNode body = findNamedChild(node, kind == JavaTypeKind.INTERFACE ? "interface_body"
                : kind == JavaTypeKind.ENUM ? "enum_body" : "class_body");

        List<JavaField> fields = new ArrayList<>();
        List<JavaMethod> methods = new ArrayList<>();
        if (body != null) {
            extractFieldsAndMethods(body, source, fields, methods);
        }

        return new JavaTypeDeclaration(
                packageName, simpleName, fqn, kind,
                Collections.unmodifiableSet(modifiers),
                superclass,
                Collections.unmodifiableList(interfaces),
                Collections.unmodifiableList(fields),
                Collections.unmodifiableList(methods),
                Collections.unmodifiableList(annotations),
                Collections.unmodifiableMap(new LinkedHashMap<>(importMap)),
                sourcePath);
    }

    private static String extractIdentifier(TreeSitterNode node, String source) {
        TreeSitterNode id = findNamedChild(node, "identifier");
        return id != null ? id.text(source) : "";
    }

    private static void extractModifiersAndAnnotations(TreeSitterNode node, String source,
            Set<JavaModifier> modifiers, List<AnnotationInfo> annotations) {

        TreeSitterNode modsNode = findNamedChild(node, "modifiers");
        if (modsNode == null) {
            return;
        }

        for (int i = 0; i < modsNode.childCount(); i++) {
            TreeSitterNode child = modsNode.child(i);
            String type = child.type();

            if ("marker_annotation".equals(type) || "annotation".equals(type)) {
                JavaAnnotation javaAnn = JavaAstHelper.parseAnnotation(child, source);
                annotations.add(toAnnotationInfo(javaAnn));
            } else {
                JavaModifier mod = parseModifierKeyword(type);
                if (mod != null) {
                    modifiers.add(mod);
                }
            }
        }
    }

    private static JavaModifier parseModifierKeyword(String keyword) {
        return switch (keyword) {
            case "public" -> JavaModifier.PUBLIC;
            case "protected" -> JavaModifier.PROTECTED;
            case "private" -> JavaModifier.PRIVATE;
            case "static" -> JavaModifier.STATIC;
            case "final" -> JavaModifier.FINAL;
            case "abstract" -> JavaModifier.ABSTRACT;
            case "synchronized" -> JavaModifier.SYNCHRONIZED;
            case "native" -> JavaModifier.NATIVE;
            case "default" -> JavaModifier.DEFAULT;
            default -> null;
        };
    }

    private static String extractSuperclass(TreeSitterNode node, String source) {
        TreeSitterNode superNode = findNamedChild(node, "superclass");
        if (superNode == null) {
            return null;
        }
        TreeSitterNode typeId = findNamedChild(superNode, "type_identifier");
        return typeId != null ? typeId.text(source) : null;
    }

    private static List<String> extractInterfaces(TreeSitterNode node, String source, JavaTypeKind kind) {
        String childType = kind == JavaTypeKind.INTERFACE ? "extends_interfaces" : "super_interfaces";
        TreeSitterNode ifacesNode = findNamedChild(node, childType);
        if (ifacesNode == null) {
            return List.of();
        }

        TreeSitterNode typeList = findNamedChild(ifacesNode, "type_list");
        if (typeList == null) {
            return List.of();
        }

        List<String> interfaces = new ArrayList<>();
        for (int i = 0; i < typeList.namedChildCount(); i++) {
            TreeSitterNode child = typeList.namedChild(i);
            if ("type_identifier".equals(child.type())) {
                interfaces.add(child.text(source));
            }
        }
        return interfaces;
    }

    private static void extractFieldsAndMethods(TreeSitterNode body, String source,
            List<JavaField> fields, List<JavaMethod> methods) {

        for (int i = 0; i < body.namedChildCount(); i++) {
            TreeSitterNode child = body.namedChild(i);
            switch (child.type()) {
                case "field_declaration" -> fields.add(extractField(child, source));
                case "method_declaration" -> methods.add(extractMethod(child, source));
                case "enum_body_declarations" -> extractFieldsAndMethods(child, source, fields, methods);
                default -> {
                }
            }
        }
    }

    private static JavaField extractField(TreeSitterNode fieldNode, String source) {
        Set<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        List<AnnotationInfo> annotations = new ArrayList<>();
        extractModifiersAndAnnotations(fieldNode, source, modifiers, annotations);

        String typeName = "";
        String fieldName = "";

        for (int i = 0; i < fieldNode.namedChildCount(); i++) {
            TreeSitterNode child = fieldNode.namedChild(i);
            switch (child.type()) {
                case "type_identifier", "integral_type", "floating_point_type", "boolean_type",
                        "void_type", "generic_type", "array_type", "scoped_type_identifier" ->
                    typeName = child.text(source);
                case "variable_declarator" -> {
                    TreeSitterNode nameNode = findNamedChild(child, "identifier");
                    if (nameNode != null) {
                        fieldName = nameNode.text(source);
                    }
                }
                default -> {
                }
            }
        }

        return new JavaField(fieldName, typeName,
                Collections.unmodifiableSet(modifiers),
                Collections.unmodifiableList(annotations));
    }

    private static JavaMethod extractMethod(TreeSitterNode methodNode, String source) {
        Set<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        List<AnnotationInfo> annotations = new ArrayList<>();
        extractModifiersAndAnnotations(methodNode, source, modifiers, annotations);

        String returnType = "";
        String methodName = "";
        List<JavaParameter> parameters = new ArrayList<>();

        for (int i = 0; i < methodNode.namedChildCount(); i++) {
            TreeSitterNode child = methodNode.namedChild(i);
            switch (child.type()) {
                case "type_identifier", "integral_type", "floating_point_type", "boolean_type",
                        "void_type", "generic_type", "array_type", "scoped_type_identifier" ->
                    returnType = child.text(source);
                case "identifier" -> methodName = child.text(source);
                case "formal_parameters" -> extractParameters(child, source, parameters);
                default -> {
                }
            }
        }

        return new JavaMethod(methodName, returnType,
                Collections.unmodifiableList(parameters),
                Collections.unmodifiableSet(modifiers),
                Collections.unmodifiableList(annotations));
    }

    private static void extractParameters(TreeSitterNode paramsNode, String source,
            List<JavaParameter> parameters) {

        for (int i = 0; i < paramsNode.namedChildCount(); i++) {
            TreeSitterNode param = paramsNode.namedChild(i);
            if (!"formal_parameter".equals(param.type()) && !"spread_parameter".equals(param.type())) {
                continue;
            }

            String paramType = "";
            String paramName = "";
            for (int j = 0; j < param.namedChildCount(); j++) {
                TreeSitterNode child = param.namedChild(j);
                switch (child.type()) {
                    case "type_identifier", "integral_type", "floating_point_type", "boolean_type",
                            "generic_type", "array_type", "scoped_type_identifier" ->
                        paramType = child.text(source);
                    case "identifier" -> paramName = child.text(source);
                    default -> {
                    }
                }
            }
            parameters.add(new JavaParameter(paramName, paramType));
        }
    }

    private static AnnotationInfo toAnnotationInfo(JavaAnnotation ann) {
        return new AnnotationInfo(ann.name(), ann.arguments(), "", ann.startRow() + 1, ann.startColumn() + 1);
    }

    static TreeSitterNode findNamedChild(TreeSitterNode node, String type) {
        for (int i = 0; i < node.namedChildCount(); i++) {
            TreeSitterNode child = node.namedChild(i);
            if (type.equals(child.type())) {
                return child;
            }
        }
        return null;
    }
}
