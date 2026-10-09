package dev.snowdrop.mtool.model.codeanalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompilationUnit {

    private String filePath;
    private String packageName;
    private List<String> imports = new ArrayList<>();
    private Map<String, TypeDeclaration> typeDeclarations = new LinkedHashMap<>();

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public Map<String, TypeDeclaration> getTypeDeclarations() {
        return typeDeclarations;
    }

    public void setTypeDeclarations(Map<String, TypeDeclaration> typeDeclarations) {
        this.typeDeclarations = typeDeclarations;
    }
}