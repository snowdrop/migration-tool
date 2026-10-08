package dev.snowdrop.mtool.model.validate.persistence;

import java.util.ArrayList;
import java.util.List;

public class FieldModel {

    private String name;
    private String type;
    private String column;
    private Boolean nullable;
    private Boolean unique;
    private boolean transientField;
    private List<String> annotations = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Boolean getUnique() {
        return unique;
    }

    public void setUnique(Boolean unique) {
        this.unique = unique;
    }

    public boolean isTransientField() {
        return transientField;
    }

    public void setTransientField(boolean transientField) {
        this.transientField = transientField;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}