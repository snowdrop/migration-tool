package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
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
		QueryScanner scanner = spiRegistry.resolveScannerForQuery(config, query);
		if (scanner == null) {
			return Collections.emptyList();
		}

		final List<Match> scannerResults = scanner.scansCodeFor(config, query);
		logger.infof("Scanner %s found %d matches", scanner.getScannerType(), scannerResults.size());
		return scannerResults;
	}

}
