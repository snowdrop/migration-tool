package dev.snowdrop.mtool.tests.scanner.file;

import dev.snowdrop.mtool.model.analyze.MatchLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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