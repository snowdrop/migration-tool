package dev.snowdrop.mtool.model.codeanalysis;

import java.util.ArrayList;
import java.util.List;

public class FieldDeclaration {

    private String type;
    private List<String> variables = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}