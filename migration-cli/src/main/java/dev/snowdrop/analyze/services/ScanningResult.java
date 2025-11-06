package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.model.Rewrite;

import java.util.List;
import java.util.Map;

public class ScanningResult {
	private final boolean matchSucceeded;
	private final Map<String, List<Rewrite>> rewrites;

	public ScanningResult(boolean matchSucceeded, Map<String, List<Rewrite>> rewrites) {
		this.matchSucceeded = matchSucceeded;
		this.rewrites = rewrites;
	}

	public boolean isMatchSucceeded() {
		return matchSucceeded;
	}

	public Map<String, List<Rewrite>> getRewrites() {
		return rewrites;
	}
}
