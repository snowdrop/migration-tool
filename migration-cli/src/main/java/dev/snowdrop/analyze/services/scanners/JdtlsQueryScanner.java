package dev.snowdrop.analyze.services.scanners;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.JdtLsClient;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.mapper.DynamicDTOMapper;
import dev.snowdrop.mapper.config.QueryScannerMappingLoader;
import dev.snowdrop.mapper.config.ScannerConfig;
import dev.snowdrop.model.JavaClassDTO;
import dev.snowdrop.model.Query;
import org.eclipse.lsp4j.SymbolInformation;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scanner implementation for JDTLS-based Java queries.
 * Handles queries like java.class, java.package using JDTLS language server.
 */
public class JdtlsQueryScanner implements QueryScanner {

	private static final Logger logger = Logger.getLogger(JdtlsQueryScanner.class);

	// Singleton pattern for managing JDT-LS server instance
	private static JdtLsClient jdtLsClientInstance;
	private static boolean isStarted = false;
	private static final Object lock = new Object();
	private final QueryScannerMappingLoader queryScannerMappingLoader;

	public JdtlsQueryScanner() {
		this.queryScannerMappingLoader = new QueryScannerMappingLoader();
	}

	@Override
	public List<Match> executeQueries(Config config, Set<Query> queries) {
		logger.infof("JDTLS scanner executing %d queries", queries.size());

		List<Match> allResults = new ArrayList<>();

		for (Query query : queries) {
			// Get scanner configuration for this query
			ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());

			// Validate that this query should be handled by JDTLS scanner
			if (!"jdtls".equals(scannerConfig.getScanner())) {
				logger.warnf("Query %s.%s is configured for scanner '%s', not 'jdtls'. Skipping.", query.fileType(),
						query.symbol(), scannerConfig.getScanner());
				continue;
			}

			// Log the DTO class that should be used for this query
			String dtoClassName = scannerConfig.getDto();
			logger.debugf("Query %s.%s will use DTO: %s", query.fileType(), query.symbol(), dtoClassName);

			// TODO: As the name of the class is know here, do we need to get it using scannerConfig.getDto();
			List<Match> queryResults = executeQuery(config, query, DynamicDTOMapper.mapToDTO(query, dtoClassName));
			allResults.addAll(queryResults);

			logger.debugf("Found %d matches for query %s.%s (DTO: %s)", queryResults.size(), query.fileType(),
					query.symbol(), dtoClassName);
		}

		// We can now stop the server
		shutdown();

		logger.infof("JDTLS scanner completed. Total matches found: %d", allResults.size());
		return allResults;
	}

	@Override
	public String getScannerType() {
		return "jdtls";
	}

	@Override
	public boolean supports(Query query) {
		// Check the configuration to see if this query should use the JDTLS scanner
		ScannerConfig scannerConfig = queryScannerMappingLoader.getScannerConfig(query.fileType(), query.symbol());
		return "jdtls".equals(scannerConfig.getScanner());
	}

	private List<Match> executeQuery(Config config, Query query, JavaClassDTO dto) {
		List<Match> results = new ArrayList<>();

		logger.infof("Executing JDTLS query: %s.%s", query.fileType(), query.symbol());

		try {
			// Get the singleton JDT-LS client instance (lazy initialization)
			JdtLsClient jdtLsClient = getJdtLsClient(config);

			logger.infof("JDT-LS server is ready, query processing for %s.%s", query.fileType(), query.symbol());
			List<SymbolInformation> symbolResults = jdtLsClient.executeCommand(config, query, dto);

			// TODO: As there is no matchId created for a JDTLS query, we will use the type+symbol. To be reviewed
			var matchId = String.format("%s-%s", query.fileType(), query.symbol());
			results.add(new Match(matchId, getScannerType(), symbolResults));

		} catch (Exception e) {
			logger.errorf("Failed to execute JDTLS query %s.%s: %s", query.fileType(), query.symbol(), e.getMessage(),
					e);
		}
		return results;
	}

	/**
	 * Gets the singleton JDT-LS client instance, initializing it if necessary.
	 * Thread-safe implementation using double-checked locking.
	 */
	private static JdtLsClient getJdtLsClient(Config config) throws Exception {
		if (!isStarted) {
			synchronized (lock) {
				if (!isStarted) {
					logger.info("Initializing JDT-LS server singleton...");
					jdtLsClientInstance = new JdtLsClient.JdtLsClientBuilder().withConfig(config).build();
					jdtLsClientInstance.start();
					isStarted = true;
					logger.info("JDT-LS server singleton initialized successfully");
				}
			}
		}
		return jdtLsClientInstance;
	}

	/**
	 * Shuts down the singleton JDT-LS server instance.
	 * This should be called when the application is shutting down.
	 */
	public static void shutdown() {
		synchronized (lock) {
			if (isStarted && jdtLsClientInstance != null) {
				logger.info("Shutting down JDT-LS server singleton...");
				jdtLsClientInstance.stop();
				jdtLsClientInstance = null;
				isStarted = false;
				logger.info("JDT-LS server singleton shut down successfully");
			}
		}
	}
}