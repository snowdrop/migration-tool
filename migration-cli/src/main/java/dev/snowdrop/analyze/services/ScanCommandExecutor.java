package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.service.scanner.QueryScanner;
import dev.snowdrop.parser.Query;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;

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
		QueryScanner scanner = spiRegistry.findScannerFor(query);
		if (scanner != null) {
			List<Match> scannerResults = scanner.scansCodeFor(config, query);
			logger.infof("Scanner %s found %d matches", scanner.getScannerType(), scannerResults.size());
			return scannerResults;
		} else {
			logger.warnf("No scanner implementation found for query: %s", query.fileType(), query.symbol());
		}

		return Collections.emptyList();
	}

}
