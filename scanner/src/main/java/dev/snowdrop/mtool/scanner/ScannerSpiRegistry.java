package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.parser.Query;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class ScannerSpiRegistry {

	private static final Logger logger = Logger.getLogger(ScannerSpiRegistry.class);
	private final Map<String, QueryScanner> scanners;

	public ScannerSpiRegistry() {
		this.scanners = new HashMap<>();
		ServiceLoader<QueryScanner> loaded = ServiceLoader.load(QueryScanner.class);

		for (QueryScanner scanner : loaded) {
			String name = scanner.getScannerType();
			scanners.put(name, scanner);
		}
		if (scanners.isEmpty()) {
			logger.warn("No QueryScanners found via SPI");
		}
	}

	public QueryScanner findScannerFor(Query query) {
		return scanners.values().stream().filter(queryScanner -> queryScanner.supports(query)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"No scanner supports query: " + query.fileType() + "." + query.symbol()));
	}

	public Optional<QueryScanner> findScanner(String name) {
		return Optional.ofNullable(scanners.get(name));
	}

	public Map<String, QueryScanner> getScanners() {
		return scanners;
	}

	/**
	 * Finds the appropriate scanner for the query based on configuration priority:
	 * 1. The default scanner (config.scanner) if it has been defined and if it supports the query.
	 * 2. Any other scanner supporting the query (excluding the default one).
	 *
	 * @param query The query (fileType and symbol) to check support for.
	 * @param config The application configuration containing the default scanner name.
	 * @return The resolved QueryScanner.
	 * @throws IllegalArgumentException if no scanner can be found that supports the query.
	 */
	public QueryScanner resolveScannerForQuery(Query query, Config config) {
		String configuredScannerName = config.scanner();

		// --- Priority 1: Scanner defined as config parameter
		if (configuredScannerName != null) {
			Optional<QueryScanner> optConfiguredScanner = findScanner(configuredScannerName);

			if (optConfiguredScanner.isPresent() && optConfiguredScanner.get().supports(query)) {
				QueryScanner configuredScanner = optConfiguredScanner.get();
				logger.infof("Using the scanner %s configured for query %s.%s", configuredScanner.getScannerType(),
						query.fileType(), query.symbol());
				return configuredScanner;
			}
		}

		// --- Priority 2: Find the first scanner supporting the query except the "config scanner"
		Collection<QueryScanner> allScanners = getScanners().values();

		Optional<QueryScanner> fallbackScanner = allScanners.stream()
				// Exclude the config scanner (as already processed)
				.filter(qs -> configuredScannerName == null || !qs.getScannerType().equals(configuredScannerName))
				.filter(qs -> qs.supports(query)).findFirst();

		if (fallbackScanner.isPresent()) {
			QueryScanner resolvedScanner = fallbackScanner.get();
			logger.infof("Using fallback scanner %s for query %s.%s (P2)", resolvedScanner.getScannerType(),
					query.fileType(), query.symbol());
			return resolvedScanner;
		}

		return fallbackScanner.orElseThrow(() -> new IllegalArgumentException("No scanner supports query: "
				+ query.fileType() + "." + query.symbol()
				+ (configuredScannerName != null
						? " (Configured scanner '" + configuredScannerName + "' was skipped or does not support it.)"
						: "")));
	}
}
