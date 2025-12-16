package dev.snowdrop.mtool.model.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MigrationTask {
	private Rule rule;
	private Rule.Instruction instruction;
	private List<Match> matchResults;

	@Deprecated
	private List<SymbolInformation> lsResults;
	@Deprecated
	private List<Match> rewriteResults;

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

	public MigrationTask withMatchResults(List<Match> matchResults) {
		this.matchResults = matchResults;
		return this;
	}

	public Rule getRule() {
		return rule;
	}

	public List<Match> getMatchResults() {
		return matchResults;
	}

	@Deprecated
	public MigrationTask withLsResults(List<SymbolInformation> lsResults) {
		this.lsResults = lsResults;
		return this;
	}

	@Deprecated
	public MigrationTask withRewriteResults(List<Match> rewriteResults) {
		this.rewriteResults = rewriteResults;
		return this;
	}

	@Deprecated
	public List<SymbolInformation> getLsResults() {
		return lsResults;
	}

	@Deprecated
	public List<Match> getRewriteResults() {
		return matchResults;
	}
}
