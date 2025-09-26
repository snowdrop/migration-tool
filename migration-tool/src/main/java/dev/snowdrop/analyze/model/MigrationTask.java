package dev.snowdrop.analyze.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MigrationTask {
    private Rule rule;
    private List<SymbolInformation> results;
    private Rule.Instruction instruction;

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

    public MigrationTask withResults(List<SymbolInformation> results) {
        this.results = results;
        return this;
    }

    public Rule getRule() {
        return rule;
    }
    public List<SymbolInformation> getResults() {
        return results;
    }
}
