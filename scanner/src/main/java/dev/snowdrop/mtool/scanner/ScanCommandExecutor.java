package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import org.jboss.logging.Logger;

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

		QueryScanner scanner = spiRegistry.resolveScannerForQuery(query, config);
		List<Match> scannerResults = scanner.scansCodeFor(config, query);
		logger.infof("Scanner %s found %d matches", scanner.getScannerType(), scannerResults.size());
		return scannerResults;
	}

}
