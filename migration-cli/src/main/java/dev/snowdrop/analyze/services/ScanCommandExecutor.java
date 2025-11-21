package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.services.scanners.*;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.model.Query;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScanCommandExecutor {

	private static final Logger logger = Logger.getLogger(ScanCommandExecutor.class);

	private final Map<String, QueryScanner> scannerRegistry;
	private final QueryScannerMappingLoader queryScannerMappingLoader;

	public ScanCommandExecutor() {
		this.scannerRegistry = initializeScanners();
		this.queryScannerMappingLoader = new QueryScannerMappingLoader();
	}

	public List<Match> executeQueryCommand(Config config, Set<Query> queries) {
		logger.infof("Executing %d queries with configurable routing", queries.size());

		// Group queries by scanner type based on YAML configuration
		Map<String, Set<Query>> queriesByScanner = groupQueriesByScanner(queries);

		// Execute each group with its appropriate scanner
		List<Match> allResults = new ArrayList<>();
		for (Map.Entry<String, Set<Query>> entry : queriesByScanner.entrySet()) {
			String scannerType = entry.getKey();
			Set<Query> scannerQueries = entry.getValue();

			QueryScanner scanner = scannerRegistry.get(scannerType);
			if (scanner != null) {
				logger.infof("Executing %d queries using %s scanner", scannerQueries.size(), scannerType);
				// Pass null as the DTO parameter for now - this could be configured based on scanner type
				@SuppressWarnings("unchecked")
				List<Match> scannerResults = scanner.executeQueries(config, scannerQueries);
				allResults.addAll(scannerResults);
				logger.infof("Scanner %s found %d matches", scannerType, scannerResults.size());
			} else {
				logger.warnf("No scanner implementation found for type: %s", scannerType);
			}
		}

		logger.infof("Total matches found: %d", allResults.size());
		return allResults;
	}

	/**
	 * Initializes the scanner registry with all available scanner implementations.
	 *
	 * @return map of scanner type to scanner implementation
	 */
	private Map<String, QueryScanner> initializeScanners() {
		Map<String, QueryScanner> registry = new HashMap<>();

		registry.put("jdtls", new JdtlsQueryScanner());
		registry.put("openrewrite", new OpenRewriteQueryScanner());
		registry.put("maven", new MavenQueryScanner());

		logger.infof("Initialized %d scanner implementations", registry.size());
		return registry;
	}

	/**
	 * Groups queries by their scanner type based on YAML configuration mapping.
	 *
	 * @param queries the queries to group
	 * @return map of scanner type to queries for that scanner
	 */
	private Map<String, Set<Query>> groupQueriesByScanner(Set<Query> queries) {
		Map<String, Set<Query>> grouped = new HashMap<>();

		for (Query query : queries) {
			ScannerConfig config = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
			String scannerType = config.getScanner();

			grouped.computeIfAbsent(scannerType, k -> new HashSet<>()).add(query);

			logger.debugf("Query %s.%s mapped to scanner: %s", query.fileType(), query.symbol(), scannerType);
		}

		logger.infof("Grouped %d queries into %d scanner types", queries.size(), grouped.size());
		return grouped;
	}
}
