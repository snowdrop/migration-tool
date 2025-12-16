package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.parser.Query;
import org.jboss.logging.Logger;

import java.util.*;

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
}
