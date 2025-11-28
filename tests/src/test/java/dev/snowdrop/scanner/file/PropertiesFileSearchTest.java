package dev.snowdrop.scanner.file;

import dev.snowdrop.scanner.file.search.model.FilePath;
import dev.snowdrop.scanner.file.search.model.MatchLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesFileSearchTest extends AbstractFileSearchTest {

	@Test
	@DisplayName("Should find the spring properties for datasource and jpa in application.properties")
	void testContentSearchForSpringProperties() {
		// Search Pattern: Lines starting with: spring.datasource OR spring.jpa
		String regex = "^\\s*(spring\\.datasource|spring\\.jpa).*$";

		List<MatchLocation> results = contentSearcher.findPropertiesMatches(applicationPath, "properties", regex);

		// Expect: spring.datasource (Lines 5 - 8), spring.jpa (Lines 11-13)
		assertThat(results).hasSize(7);

		// Verify line numbers and content
		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8, 11, 12, 13);

		assertThat(results).extracting(MatchLocation::lineContent).contains(
				"spring.datasource.url=jdbc:mysql://127.0.0.1:3306/todo", "spring.datasource.username=root",
				"spring.datasource.password=root", "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
				"spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=true",
				"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect");
	}

	@Test
	@DisplayName("Should find spring properties using recursive search")
	void testSearchForMigrationProperties() {
		String regex = "^\\s*spring\\.[a-zA-Z0-9\\.\\-]+=.*$";

		List<MatchLocation> results = contentSearcher.findPropertiesMatches(applicationPath, "properties", regex);

		assertThat(results).hasSize(7);
		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(5, 6, 7, 8, 11, 12, 13);
	}

}