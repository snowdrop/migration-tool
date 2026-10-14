package dev.snowdrop.mtool.scanner.fluent;

import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaModifier;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class FieldQuery {

    private final List<JavaTypeDeclaration> source;
    private final List<Predicate<FieldResult>> filters = new ArrayList<>();

    public FieldQuery(List<JavaTypeDeclaration> source) {
        this.source = source;
    }

    public FieldQuery ofType(String typeName) {
        filters.add(fr -> typeName.equals(fr.field().typeName()));
        return this;
    }

    public FieldQuery annotatedWith(String annotationName) {
        filters.add(fr -> fr.field().annotations().stream()
                .anyMatch(a -> annotationName.equals(a.name())
                        || annotationName.equals(fr.declaringType().resolveType(a.name()))));
        return this;
    }

    public FieldQuery annotatedWith(String annotationName, String key, String value) {
        filters.add(fr -> fr.field().annotations().stream()
                .anyMatch(a -> (annotationName.equals(a.name())
                        || annotationName.equals(fr.declaringType().resolveType(a.name())))
                        && a.hasArgument(key, value)));
        return this;
    }

    public FieldQuery annotatedWithFqn(String fqn) {
        filters.add(fr -> fr.field().annotations().stream()
                .anyMatch(a -> fqn.equals(fr.declaringType().resolveType(a.name()))));
        return this;
    }

    public FieldQuery withModifier(JavaModifier modifier) {
        filters.add(fr -> fr.field().modifiers().contains(modifier));
        return this;
    }

    public FieldQuery inType(Predicate<JavaTypeDeclaration> typePredicate) {
        filters.add(fr -> typePredicate.test(fr.declaringType()));
        return this;
    }

    public List<FieldResult> toList() {
        return allFieldResults().stream()
                .filter(fr -> filters.stream().allMatch(f -> f.test(fr)))
                .toList();
    }

    public int count() {
        return (int) allFieldResults().stream()
                .filter(fr -> filters.stream().allMatch(f -> f.test(fr)))
                .count();
    }

    private List<FieldResult> allFieldResults() {
        List<FieldResult> results = new ArrayList<>();
        for (JavaTypeDeclaration type : source) {
            for (JavaField field : type.fields()) {
                results.add(new FieldResult(field, type));
            }
        }
        return results;
    }
}
