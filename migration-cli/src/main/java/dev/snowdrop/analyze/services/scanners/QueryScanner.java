package dev.snowdrop.analyze.services.scanners;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.model.Query;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for different query scanner implementations.
 * Each scanner type (OpenRewrite, Maven, Gradle, JDTLS, File) implements this interface.
 *
 */
public interface QueryScanner {

	/**
	 * Executes the given queries using this scanner's implementation.
	 * All queries in the set should be of the same type that this scanner supports.
	 *
	 * @param config  the configuration context
	 * @param queries the set of homogeneous queries to execute
	 * @return list of matches found by this scanner
	 */
	@Deprecated
	List<Match> executeQueries(Config config, Set<Query> queries);

	List<Match> scansCodeFor(Config config, Query query);

	/**
	 * Returns the scanner type identifier.
	 *
	 * @return the scanner type (e.g., "openrewrite", "maven", "gradle", "jdtls", "file")
	 */
	String getScannerType();

	/**
	 * Indicates whether this scanner supports the given query type.
	 *
	 * @param query the query to check
	 * @return true if this scanner can handle the query
	 */
	boolean supports(Query query);
}