package dev.snowdrop.mtool.model.java;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record JavaTypeDeclaration(
        String packageName,
        String simpleName,
        String fqn,
        JavaTypeKind kind,
        Set<JavaModifier> modifiers,
        String superclass,
        List<String> interfaces,
        List<JavaField> fields,
        List<JavaMethod> methods,
        List<AnnotationInfo> annotations,
        Map<String, String> importMap,
        String sourcePath) {

    public String resolveType(String simpleName) {
        return importMap.getOrDefault(simpleName, simpleName);
    }

    public String superclassFqn() {
        return superclass != null ? resolveType(superclass) : null;
    }

    public List<String> interfaceFqns() {
        return interfaces.stream().map(this::resolveType).toList();
    }
}
