package dev.snowdrop.mtool.scanner.javaparser.index;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import dev.snowdrop.mtool.model.analyze.AnnotationInfo;
import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaParameter;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaParserTypeExtractor {

    private JavaParserTypeExtractor() {
    }

    public static List<JavaTypeDeclaration> extract(JavaSource source) {
        CompilationUnit cu = StaticJavaParser.parse(source.sourceCode());
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");
        Map<String, String> importMap = buildImportMap(cu);

        List<JavaTypeDeclaration> declarations = new ArrayList<>();
        for (TypeDeclaration<?> type : cu.getTypes()) {
            declarations.add(extractTypeDeclaration(type, packageName, importMap, source.path()));
        }
        return declarations;
    }

    private static Map<String, String> buildImportMap(CompilationUnit cu) {
        Map<String, String> importMap = new LinkedHashMap<>();
        for (ImportDeclaration imp : cu.getImports()) {
            if (!imp.isAsterisk()) {
                String fqn = imp.getNameAsString();
                String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
                importMap.put(simpleName, fqn);
            }
        }
        return importMap;
    }

    private static JavaTypeDeclaration extractTypeDeclaration(TypeDeclaration<?> type,
            String packageName, Map<String, String> importMap, String sourcePath) {

        String simpleName = type.getNameAsString();
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        JavaTypeKind kind = resolveKind(type);

        Set<JavaModifier> modifiers = extractModifiers(type.getModifiers());
        List<AnnotationInfo> annotations = extractAnnotations(type.getAnnotations());

        String superclass = null;
        List<String> interfaces = List.of();

        if (type instanceof ClassOrInterfaceDeclaration cid) {
            if (!cid.isInterface()) {
                superclass = cid.getExtendedTypes().stream()
                        .findFirst()
                        .map(ClassOrInterfaceType::getNameAsString)
                        .orElse(null);
                interfaces = cid.getImplementedTypes().stream()
                        .map(ClassOrInterfaceType::getNameAsString)
                        .toList();
            } else {
                interfaces = cid.getExtendedTypes().stream()
                        .map(ClassOrInterfaceType::getNameAsString)
                        .toList();
            }
        } else if (type instanceof EnumDeclaration ed) {
            interfaces = ed.getImplementedTypes().stream()
                    .map(ClassOrInterfaceType::getNameAsString)
                    .toList();
        }

        List<JavaField> fields = new ArrayList<>();
        List<JavaMethod> methods = new ArrayList<>();

        for (FieldDeclaration fd : type.getFields()) {
            for (VariableDeclarator var : fd.getVariables()) {
                fields.add(new JavaField(
                        var.getNameAsString(),
                        fd.getElementType().asString(),
                        Collections.unmodifiableSet(extractModifiers(fd.getModifiers())),
                        Collections.unmodifiableList(extractAnnotations(fd.getAnnotations()))));
            }
        }

        for (MethodDeclaration md : type.getMethods()) {
            List<JavaParameter> params = md.getParameters().stream()
                    .map(p -> new JavaParameter(p.getNameAsString(), p.getTypeAsString()))
                    .toList();

            methods.add(new JavaMethod(
                    md.getNameAsString(),
                    md.getTypeAsString(),
                    Collections.unmodifiableList(params),
                    Collections.unmodifiableSet(extractModifiers(md.getModifiers())),
                    Collections.unmodifiableList(extractAnnotations(md.getAnnotations()))));
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

    private static JavaTypeKind resolveKind(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration cid) {
            return cid.isInterface() ? JavaTypeKind.INTERFACE : JavaTypeKind.CLASS;
        } else if (type instanceof EnumDeclaration) {
            return JavaTypeKind.ENUM;
        } else if (type instanceof com.github.javaparser.ast.body.RecordDeclaration) {
            return JavaTypeKind.RECORD;
        } else if (type instanceof com.github.javaparser.ast.body.AnnotationDeclaration) {
            return JavaTypeKind.ANNOTATION_TYPE;
        }
        return JavaTypeKind.CLASS;
    }

    private static Set<JavaModifier> extractModifiers(com.github.javaparser.ast.NodeList<Modifier> modifiers) {
        Set<JavaModifier> result = EnumSet.noneOf(JavaModifier.class);
        for (Modifier mod : modifiers) {
            JavaModifier mapped = mapModifier(mod.getKeyword());
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    private static JavaModifier mapModifier(Modifier.Keyword keyword) {
        return switch (keyword) {
            case PUBLIC -> JavaModifier.PUBLIC;
            case PROTECTED -> JavaModifier.PROTECTED;
            case PRIVATE -> JavaModifier.PRIVATE;
            case STATIC -> JavaModifier.STATIC;
            case FINAL -> JavaModifier.FINAL;
            case ABSTRACT -> JavaModifier.ABSTRACT;
            case SYNCHRONIZED -> JavaModifier.SYNCHRONIZED;
            case NATIVE -> JavaModifier.NATIVE;
            case DEFAULT -> JavaModifier.DEFAULT;
            default -> null;
        };
    }

    private static List<AnnotationInfo> extractAnnotations(
            com.github.javaparser.ast.NodeList<AnnotationExpr> annotations) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (AnnotationExpr ann : annotations) {
            result.add(toAnnotationInfo(ann));
        }
        return result;
    }

    private static AnnotationInfo toAnnotationInfo(AnnotationExpr ann) {
        String name = ann.getNameAsString();
        Map<String, String> arguments = new LinkedHashMap<>();

        if (ann instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                arguments.put(pair.getNameAsString(), pair.getValue().toString());
            }
        } else if (ann instanceof SingleMemberAnnotationExpr single) {
            arguments.put("value", single.getMemberValue().toString());
        }

        int line = ann.getBegin().map(p -> p.line).orElse(0);
        int column = ann.getBegin().map(p -> p.column).orElse(0);

        return new AnnotationInfo(name, arguments, "", line, column);
    }
}
