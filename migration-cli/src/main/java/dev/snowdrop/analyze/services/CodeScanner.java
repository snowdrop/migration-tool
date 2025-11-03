package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.model.MigrationTask;
import dev.snowdrop.analyze.model.Rule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CodeScanner {
	Map<String, MigrationTask> analyze(List<Rule> rules) throws IOException;
}