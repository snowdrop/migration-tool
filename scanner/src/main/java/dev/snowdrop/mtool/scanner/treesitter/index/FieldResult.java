package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

public record FieldResult(JavaField field, JavaTypeDeclaration declaringType) {
}
