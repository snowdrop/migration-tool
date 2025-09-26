package dev.snowdrop.openrewrite.model;

import org.eclipse.lsp4j.SymbolKind;

public record Result(String name, SymbolKind kind, Location location, String containerName) {
}
