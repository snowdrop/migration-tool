package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.services.scanners.QueryScanner;
import dev.snowdrop.parser.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ScannerSpiRegistry {

	private final List<QueryScanner> scanners;

	public ScannerSpiRegistry() {
		this.scanners = new ArrayList<>();
		ServiceLoader.load(QueryScanner.class).forEach(scanners::add);
	}

	public QueryScanner findScannerFor(Query query) {
		return scanners.stream().filter(queryScanner -> queryScanner.supports(query)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"No scanner supports query: " + query.fileType() + "." + query.symbol()));
	}

}
