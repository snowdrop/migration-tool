package dev.snowdrop.mtool.model.validate.persistence;

import java.util.ArrayList;
import java.util.List;

public class EntityModel {

    private String originalFile;
    private String packageName;
    private String className;
    private String tableName;
    private List<String> annotations = new ArrayList<>();
    private List<String> extendsClasses = new ArrayList<>();
    private List<String> implementsInterfaces = new ArrayList<>();
    private List<FieldModel> fields = new ArrayList<>();
    private List<RelationshipModel> relationships = new ArrayList<>();
    private IdGenerationModel idGeneration;

    public String getOriginalFile() {
        return originalFile;
    }

    public void setOriginalFile(String originalFile) {
        this.originalFile = originalFile;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<String> getExtendsClasses() {
        return extendsClasses;
    }

    public void setExtendsClasses(List<String> extendsClasses) {
        this.extendsClasses = extendsClasses;
    }

    public List<String> getImplementsInterfaces() {
        return implementsInterfaces;
    }

    public void setImplementsInterfaces(List<String> implementsInterfaces) {
        this.implementsInterfaces = implementsInterfaces;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }

    public List<RelationshipModel> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipModel> relationships) {
        this.relationships = relationships;
    }

    public IdGenerationModel getIdGeneration() {
        return idGeneration;
    }

    public void setIdGeneration(IdGenerationModel idGeneration) {
        this.idGeneration = idGeneration;
    }
}