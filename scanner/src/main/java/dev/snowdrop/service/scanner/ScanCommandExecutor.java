package dev.snowdrop.service.scanner;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.parser.Query;
import org.jboss.logging.Logger;

import java.util.Collections;
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
		}

		List<Match> scannerResults = scanner.scansCodeFor(config, query);
		logger.infof("Scanner %s found %d matches", scanner.getScannerType(), scannerResults.size());
		return scannerResults;

	}

}
