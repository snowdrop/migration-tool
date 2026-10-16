package dev.snowdrop.mtool.model.codeanalysis;

import java.util.ArrayList;
import java.util.List;

public class TypeDeclaration {

    private boolean isInterface;
    private List<String> annotations = new ArrayList<>();
    private List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
    private List<String> extendsList = new ArrayList<>();
    private List<String> implementsList = new ArrayList<>();

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<FieldDeclaration> getFieldDeclarations() {
        return fieldDeclarations;
    }

    public void setFieldDeclarations(List<FieldDeclaration> fieldDeclarations) {
        this.fieldDeclarations = fieldDeclarations;
    }

    public List<String> getExtendsList() {
        return extendsList;
    }

    public void setExtendsList(List<String> extendsList) {
        this.extendsList = extendsList;
    }

    public List<String> getImplementsList() {
        return implementsList;
    }

    public void setImplementsList(List<String> implementsList) {
        this.implementsList = implementsList;
    }
}