package dev.snowdrop.mtool.scanner.fluent;

import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaTypeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class TypeQuery {

    private final List<JavaTypeDeclaration> source;
    private final List<Predicate<JavaTypeDeclaration>> filters = new ArrayList<>();

    public TypeQuery(List<JavaTypeDeclaration> source) {
        this.source = source;
    }

    public TypeQuery ofKind(JavaTypeKind kind) {
        filters.add(t -> t.kind() == kind);
        return this;
    }

    public TypeQuery annotatedWith(String annotationName) {
        filters.add(t -> t.annotations().stream()
                .anyMatch(a -> annotationName.equals(a.name())
                        || annotationName.equals(t.resolveType(a.name()))));
        return this;
    }

    public TypeQuery annotatedWith(String annotationName, String key, String value) {
        filters.add(t -> t.annotations().stream()
                .anyMatch(a -> (annotationName.equals(a.name())
                        || annotationName.equals(t.resolveType(a.name())))
                        && a.hasArgument(key, value)));
        return this;
    }

    public TypeQuery extending(String superclassName) {
        filters.add(t -> superclassName.equals(t.superclass())
                || superclassName.equals(t.superclassFqn()));
        return this;
    }

    public TypeQuery implementing(String interfaceName) {
        filters.add(t -> t.interfaces().contains(interfaceName)
                || t.interfaceFqns().contains(interfaceName));
        return this;
    }

    public TypeQuery inPackage(String packagePrefix) {
        filters.add(t -> t.packageName().startsWith(packagePrefix));
        return this;
    }

    public TypeQuery withModifier(JavaModifier modifier) {
        filters.add(t -> t.modifiers().contains(modifier));
        return this;
    }

    public TypeQuery withField(Predicate<JavaField> predicate) {
        filters.add(t -> t.fields().stream().anyMatch(predicate));
        return this;
    }

    public TypeQuery withMethod(Predicate<JavaMethod> predicate) {
        filters.add(t -> t.methods().stream().anyMatch(predicate));
        return this;
    }

    public List<JavaTypeDeclaration> toList() {
        return source.stream()
                .filter(t -> filters.stream().allMatch(f -> f.test(t)))
                .toList();
    }

    public Optional<JavaTypeDeclaration> first() {
        return source.stream()
                .filter(t -> filters.stream().allMatch(f -> f.test(t)))
                .findFirst();
    }

    public boolean exists() {
        return source.stream()
                .anyMatch(t -> filters.stream().allMatch(f -> f.test(t)));
    }

    public int count() {
        return (int) source.stream()
                .filter(t -> filters.stream().allMatch(f -> f.test(t)))
                .count();
    }
}
