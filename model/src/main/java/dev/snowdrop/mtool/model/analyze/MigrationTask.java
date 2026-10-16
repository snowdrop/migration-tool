package dev.snowdrop.mtool.model.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MigrationTask {
    private Rule rule;
    private Rule.Instruction instruction;
    private List<Result> matchResults;

    public MigrationTask() {
    }

    public MigrationTask withInstruction(Rule.Instruction instruction) {
        this.instruction = instruction;
        return this;
    }

    public MigrationTask withRule(Rule rule) {
        this.rule = rule;
        return this;
    }

    public MigrationTask withMatchResults(List<Result> matchResults) {
        this.matchResults = matchResults;
        return this;
    }

    public Rule getRule() {
        return rule;
    }

    public List<Result> getMatchResults() {
        return matchResults;
    }
}
