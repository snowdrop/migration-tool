package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.model.Match;

import java.util.List;
import java.util.Map;

public class ScanningResult {
	private final boolean matchSucceeded;
	private final Map<String, List<Match>> matches;

	public ScanningResult(boolean matchSucceeded, Map<String, List<Match>> rewrites) {
		this.matchSucceeded = matchSucceeded;
		this.matches = rewrites;
	}

	public boolean isMatchSucceeded() {
		return matchSucceeded;
	}

	public Map<String, List<Match>> getMatches() {
		return matches;
	}
}
