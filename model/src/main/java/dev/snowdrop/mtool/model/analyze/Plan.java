package dev.snowdrop.mtool.model.analyze;

import java.util.List;

public class Plan {
    private String name;
    private List<String> queries;

    public Plan() {
    }

    public Plan(String name, List<String> queries) {
        this.name = name;
        this.queries = queries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }
}
