package dev.snowdrop.mtool.scanner.treesitter.index;

import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

public record IndexedQueryResult(
        String captureName,
        String text,
        int startRow,
        int startColumn,
        int endRow,
        int endColumn,
        JavaTypeDeclaration declaringType,
        String sourceCode) {
}
