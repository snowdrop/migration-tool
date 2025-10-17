package dev.snowdrop.analyze.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MigrationTask {
    private Rule rule;
    private List<SymbolInformation> lsResults;
    private List<Rewrite> rewriteResults;
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

    public MigrationTask withLsResults(List<SymbolInformation> lsResults) {
        this.lsResults = lsResults;
        return this;
    }

    public MigrationTask withRewriteResults(List<Rewrite> rewriteResults) {
        this.rewriteResults = rewriteResults;
        return this;
    }

    public Rule getRule() {
        return rule;
    }

    public List<SymbolInformation> getLsResults() {
        return lsResults;
    }

    public List<Rewrite> getRewriteResults() {
        return rewriteResults;
    }
}
