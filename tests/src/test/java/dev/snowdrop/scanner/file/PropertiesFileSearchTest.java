package dev.snowdrop.scanner.file;

import dev.snowdrop.analyze.BaseRulesTest;
import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.model.Match;
import dev.snowdrop.analyze.model.MatchLocation;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.services.CodeScannerService;
import dev.snowdrop.analyze.services.ScanCommandExecutor;
import dev.snowdrop.analyze.services.scanners.FileSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.analyze.utils.YamlRuleParser.parseRulesFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesFileSearchTest extends AbstractFileSearchTest {

	@Test
	@DisplayName("Should find the spring properties for datasource in application.properties")
	void testContentSearchForSpringProperty() {
		// Search Pattern: Lines starting with: spring.datasource OR spring.jpa
		String regex = "^\\s*(spring\\.datasource).*$";

		List<MatchLocation> results = contentSearcher.findPropertiesMatches(applicationPath, "properties", regex);

		// Expect: spring.datasource (Lines 5 - 8)
		assertThat(results).hasSize(4);

		// Verify line numbers and content
		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8);

		assertThat(results).extracting(MatchLocation::lineContent).contains(
				"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
				"spring.datasource.password=root", "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
	}

}