package dev.snowdrop.mtool.scanner.fluent;

import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

public record FieldResult(JavaField field, JavaTypeDeclaration declaringType) {
}
