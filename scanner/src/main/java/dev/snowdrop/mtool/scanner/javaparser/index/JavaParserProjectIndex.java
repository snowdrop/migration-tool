package dev.snowdrop.mtool.scanner.javaparser.index;

import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.scanner.fluent.FieldQuery;
import dev.snowdrop.mtool.scanner.fluent.MethodQuery;
import dev.snowdrop.mtool.scanner.fluent.TypeQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JavaParserProjectIndex {

    private final Map<String, JavaTypeDeclaration> typesByFqn = new LinkedHashMap<>();
    private final Map<String, List<String>> fqnsBySourcePath = new LinkedHashMap<>();

    private JavaParserProjectIndex() {
    }

    public static JavaParserProjectIndex build(List<JavaSource> sources) {
        JavaParserProjectIndex index = new JavaParserProjectIndex();
        for (JavaSource source : sources) {
            index.indexSource(source);
        }
        return index;
    }

    // --- Incremental updates ---

    public void addOrUpdate(JavaSource source) {
        removeByPath(source.path());
        indexSource(source);
    }

    public void addOrUpdate(List<JavaSource> sources) {
        for (JavaSource source : sources) {
            removeByPath(source.path());
            indexSource(source);
        }
    }

    public void remove(String sourcePath) {
        removeByPath(sourcePath);
    }

    public void remove(List<String> sourcePaths) {
        sourcePaths.forEach(this::removeByPath);
    }

    // --- Direct lookups ---

    public JavaTypeDeclaration getType(String fqn) {
        return typesByFqn.get(fqn);
    }

    public List<JavaTypeDeclaration> allTypes() {
        return List.copyOf(typesByFqn.values());
    }

    // --- Navigation ---

    public JavaTypeDeclaration getSuperclass(JavaTypeDeclaration type) {
        if (type.superclass() == null) {
            return null;
        }
        return typesByFqn.get(resolveToFqn(type, type.superclass()));
    }

    public List<JavaTypeDeclaration> getSuperclassChain(JavaTypeDeclaration type) {
        List<JavaTypeDeclaration> chain = new ArrayList<>();
        JavaTypeDeclaration current = getSuperclass(type);
        while (current != null) {
            chain.add(current);
            current = getSuperclass(current);
        }
        return chain;
    }

    public List<JavaTypeDeclaration> getSubclasses(JavaTypeDeclaration type) {
        return typesByFqn.values().stream()
                .filter(t -> t.superclass() != null
                        && type.fqn().equals(resolveToFqn(t, t.superclass())))
                .toList();
    }

    public List<JavaTypeDeclaration> getImplementors(String interfaceFqn) {
        return typesByFqn.values().stream()
                .filter(t -> t.interfaces().stream()
                        .anyMatch(iface -> interfaceFqn.equals(resolveToFqn(t, iface))))
                .toList();
    }

    // --- Fluent queries ---

    public TypeQuery findTypes() {
        return new TypeQuery(allTypes());
    }

    public FieldQuery findFields() {
        return new FieldQuery(allTypes());
    }

    public MethodQuery findMethods() {
        return new MethodQuery(allTypes());
    }

    // --- Internal ---

    private void indexSource(JavaSource source) {
        List<JavaTypeDeclaration> types = JavaParserTypeExtractor.extract(source);
        List<String> fqns = new ArrayList<>();

        for (JavaTypeDeclaration type : types) {
            typesByFqn.put(type.fqn(), type);
            fqns.add(type.fqn());
        }

        fqnsBySourcePath.put(source.path(), fqns);
    }

    private void removeByPath(String sourcePath) {
        List<String> fqns = fqnsBySourcePath.remove(sourcePath);
        if (fqns != null) {
            fqns.forEach(typesByFqn::remove);
        }
    }

    private String resolveToFqn(JavaTypeDeclaration context, String simpleName) {
        if (simpleName == null) {
            return null;
        }
        if (simpleName.contains(".")) {
            return simpleName;
        }
        String fromImport = context.resolveType(simpleName);
        if (fromImport != null && fromImport.contains(".")) {
            return fromImport;
        }
        String samePackage = context.packageName().isEmpty()
                ? simpleName
                : context.packageName() + "." + simpleName;
        if (typesByFqn.containsKey(samePackage)) {
            return samePackage;
        }
        return fromImport != null ? fromImport : simpleName;
    }
}
