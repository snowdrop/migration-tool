package dev.snowdrop.mtool.model.codeanalysis;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnalysisResult {

    private Map<String, CompilationUnit> symbolTable = new LinkedHashMap<>();

    public Map<String, CompilationUnit> getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(Map<String, CompilationUnit> symbolTable) {
        this.symbolTable = symbolTable;
    }
}