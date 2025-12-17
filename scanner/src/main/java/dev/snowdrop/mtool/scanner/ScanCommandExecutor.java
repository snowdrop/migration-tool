package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ScanCommandExecutor {

	private static final Logger logger = Logger.getLogger(ScanCommandExecutor.class);

	private final ScannerSpiRegistry spiRegistry;

	public ScanCommandExecutor() {
		this.spiRegistry = new ScannerSpiRegistry();
	}

	// Constructor for tests
	public ScanCommandExecutor(ScannerSpiRegistry registry) {
		this.spiRegistry = registry;
	}

	public List<Match> executeCommandForQuery(Config config, Query query) {
		/*
		QueryScanner scanner = spiRegistry.findScannerFor(query);;
		if (config.scanner() != null) {
			Optional<QueryScanner> optScanner = spiRegistry.findScanner(config.scanner());
			if (optScanner.isPresent() && optScanner.get().supports(query)) {
				scanner = optScanner.get();
				logger.debugf("Using command configured scanner %s", scanner.getScannerType());
			} else {
				logger.infof("Using %s as scanner for analysing code for query %s.%s", config.scanner(),
						query.fileType(), query.symbol());
				return Collections.emptyList();
			}
		}*/

		QueryScanner scanner = resolveScannerForQuery(query, config);
		List<Match> scannerResults = scanner.scansCodeFor(config, query);
		logger.infof("Scanner %s found %d matches", scanner.getScannerType(), scannerResults.size());
		return scannerResults;
	}

	/**
	 * Finds the appropriate scanner for the query based on configuration priority:
	 * 1. The default scanner (config.scanner) if it has been defined and if it supports the query.
	 * 2. Any other scanner supporting the query (excluding the default one).
	 *
	 * @param query The query (fileType and symbol) to check support for.
	 * @param config The application configuration containing the default scanner name.
	 * @param spiRegistry The registry containing all available scanners.
	 * @return The resolved QueryScanner.
	 * @throws IllegalArgumentException if no scanner can be found that supports the query.
	 */
	public QueryScanner resolveScannerForQuery(Query query, Config config) {
		String configuredScannerName = config.scanner();

		// --- Priority 1: Scanner defined as config parameter
		if (configuredScannerName != null) {
			Optional<QueryScanner> optConfiguredScanner = spiRegistry.findScanner(configuredScannerName);

			if (optConfiguredScanner.isPresent() && optConfiguredScanner.get().supports(query)) {
				QueryScanner configuredScanner = optConfiguredScanner.get();
				logger.debugf("Using command configured scanner %s for query %s.%s", configuredScanner.getScannerType(),
						query.fileType(), query.symbol());
				return configuredScanner;
			}
		}

		// --- Priority 2: Find the first scanner supporting the query except the "config scanner"
		Collection<QueryScanner> allScanners = spiRegistry.getScanners().values();

		Optional<QueryScanner> fallbackScanner = allScanners.stream()
				// Exclude the config scanner (as already processed)
				.filter(qs -> configuredScannerName == null || !qs.getScannerType().equals(configuredScannerName))
				.filter(qs -> qs.supports(query)).findFirst();

		return fallbackScanner.orElseThrow(() -> new IllegalArgumentException("No scanner supports query: "
				+ query.fileType() + "." + query.symbol()
				+ (configuredScannerName != null
						? " (Configured scanner '" + configuredScannerName + "' was skipped or does not support it.)"
						: "")));
	}

}
