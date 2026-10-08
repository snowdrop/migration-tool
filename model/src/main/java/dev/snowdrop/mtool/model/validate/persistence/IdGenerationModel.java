package dev.snowdrop.mtool.model.validate.persistence;

public class IdGenerationModel {

    private String strategy;
    private String generator;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }
}