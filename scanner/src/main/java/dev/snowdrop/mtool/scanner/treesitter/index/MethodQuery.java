package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MethodQuery {

    private final List<JavaTypeDeclaration> source;
    private final List<Predicate<MethodResult>> filters = new ArrayList<>();

    MethodQuery(List<JavaTypeDeclaration> source) {
        this.source = source;
    }

    public MethodQuery named(String name) {
        filters.add(mr -> name.equals(mr.method().name()));
        return this;
    }

    public MethodQuery returningType(String typeName) {
        filters.add(mr -> typeName.equals(mr.method().returnType()));
        return this;
    }

    public MethodQuery annotatedWith(String annotationName) {
        filters.add(mr -> mr.method().annotations().stream()
                .anyMatch(a -> annotationName.equals(a.name())
                        || annotationName.equals(mr.declaringType().resolveType(a.name()))));
        return this;
    }

    public MethodQuery withModifier(JavaModifier modifier) {
        filters.add(mr -> mr.method().modifiers().contains(modifier));
        return this;
    }

    public MethodQuery inType(Predicate<JavaTypeDeclaration> typePredicate) {
        filters.add(mr -> typePredicate.test(mr.declaringType()));
        return this;
    }

    public List<MethodResult> toList() {
        return allMethodResults().stream()
                .filter(mr -> filters.stream().allMatch(f -> f.test(mr)))
                .toList();
    }

    public int count() {
        return (int) allMethodResults().stream()
                .filter(mr -> filters.stream().allMatch(f -> f.test(mr)))
                .count();
    }

    private List<MethodResult> allMethodResults() {
        List<MethodResult> results = new ArrayList<>();
        for (JavaTypeDeclaration type : source) {
            for (JavaMethod method : type.methods()) {
                results.add(new MethodResult(method, type));
            }
        }
        return results;
    }
}
