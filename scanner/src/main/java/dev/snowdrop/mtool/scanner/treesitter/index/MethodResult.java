package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaMethod;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

public record MethodResult(JavaMethod method, JavaTypeDeclaration declaringType) {
}
