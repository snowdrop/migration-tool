package dev.snowdrop.mtool.scanner;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
//@Disabled
class ScanCommandExecutorTest {

	@Mock
	ScannerSpiRegistry spiRegistry;

	@Mock
	QueryScanner scanner;

	@Mock
	Config config;

	@Test
	void executeCommandForQuery_returnsScannerResults() {
		// Given
		ScanCommandExecutor executor = new ScanCommandExecutor(spiRegistry);

		Query query = new Query("java", "annotation", Collections.emptyMap());

		List<Match> expectedResults = List.of(new Match("file1", "openrewrite", Collections.emptyList()),
				new Match("file2", "maven", Collections.emptyList()));

		when(spiRegistry.resolveScannerForQuery(config, query)).thenReturn(scanner);
		when(scanner.scansCodeFor(config, query)).thenReturn(expectedResults);

		// When
		List<Match> result = executor.executeCommandForQuery(config, query);

		// Then
		assertEquals(expectedResults, result);
		verify(scanner).scansCodeFor(config, query);
		verify(spiRegistry).resolveScannerForQuery(config, query);
	}

	@Test
	void executeCommandForQuery_returnsEmptyListWhenNoScannerFound() {
		ScanCommandExecutor executor = new ScanCommandExecutor(spiRegistry);
		Query query = new Query("java", "interface", Collections.emptyMap());

		List<Match> result = executor.executeCommandForQuery(config, query);

		assertTrue(result.isEmpty());
	}

}
