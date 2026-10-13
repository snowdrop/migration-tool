package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterQuery;
import io.roastedroot.treesitter.TreeSitterQueryResult;
import io.roastedroot.treesitter.TreeSitterTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JavaProjectIndex implements AutoCloseable {

    private final TreeSitter ts;
    private final TreeSitterParser parser;
    private final Map<String, JavaTypeDeclaration> typesByFqn = new LinkedHashMap<>();
    private final Map<String, List<String>> fqnsBySourcePath = new LinkedHashMap<>();
    private final Map<String, String> sourceCodeByPath = new LinkedHashMap<>();

    private JavaProjectIndex(TreeSitter ts) {
        this.ts = ts;
        this.parser = ts.newParser(Language.JAVA);
    }

    public static JavaProjectIndex build(TreeSitter ts, List<JavaSource> sources) {
        JavaProjectIndex index = new JavaProjectIndex(ts);
        for (JavaSource source : sources) {
            index.indexSource(source);
        }
        return index;
    }

    @Override
    public void close() {
        parser.close();
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

    // --- Cross-file tree-sitter query ---

    public List<IndexedQueryResult> queryAll(String treeSitterPattern) {
        List<IndexedQueryResult> results = new ArrayList<>();

        try (TreeSitterQuery query = ts.newQuery(Language.JAVA, treeSitterPattern)) {

            for (Map.Entry<String, String> entry : sourceCodeByPath.entrySet()) {
                String path = entry.getKey();
                String source = entry.getValue();

                try (TreeSitterTree tree = parser.parseString(source)) {
                    List<TreeSitterQueryResult> matches = query.exec(tree.rootNode(), source);
                    JavaTypeDeclaration declaringType = findDeclaringType(path);

                    for (TreeSitterQueryResult match : matches) {
                        var node = match.node();
                        results.add(new IndexedQueryResult(
                                match.name(),
                                node.text(source),
                                node.startRow(),
                                node.startColumn(),
                                node.endRow(),
                                node.endColumn(),
                                declaringType,
                                source));
                    }
                }
            }
        }

        return results;
    }

    // --- Internal ---

    private void indexSource(JavaSource source) {
        List<JavaTypeDeclaration> types = JavaTypeExtractor.extract(ts, parser, source);
        List<String> fqns = new ArrayList<>();

        for (JavaTypeDeclaration type : types) {
            typesByFqn.put(type.fqn(), type);
            fqns.add(type.fqn());
        }

        fqnsBySourcePath.put(source.path(), fqns);
        sourceCodeByPath.put(source.path(), source.sourceCode());
    }

    private void removeByPath(String sourcePath) {
        List<String> fqns = fqnsBySourcePath.remove(sourcePath);
        if (fqns != null) {
            fqns.forEach(typesByFqn::remove);
        }
        sourceCodeByPath.remove(sourcePath);
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

    private JavaTypeDeclaration findDeclaringType(String sourcePath) {
        List<String> fqns = fqnsBySourcePath.get(sourcePath);
        if (fqns != null && !fqns.isEmpty()) {
            return typesByFqn.get(fqns.get(0));
        }
        return null;
    }
}
