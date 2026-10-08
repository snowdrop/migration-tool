package dev.snowdrop.mtool.model.validate.persistence;

import java.util.List;

public class RelationshipModel {

    private String type;
    private String targetEntity;
    private String collectionType;
    private String mappedBy;
    private String fetch;
    private String column;
    private List<String> cascade;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    public String getFetch() {
        return fetch;
    }

    public void setFetch(String fetch) {
        this.fetch = fetch;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public List<String> getCascade() {
        return cascade;
    }

    public void setCascade(List<String> cascade) {
        this.cascade = cascade;
    }
}