package dev.snowdrop.scanner.file;

import dev.snowdrop.scanner.file.search.model.MatchLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileContentSearchTest extends AbstractFileSearchTest {

	@Test
	@DisplayName("Should find GetMapping annotations in Java file")
	void testContentSearch_ForRequestMapping() {

		String regex = "@GetMapping";
		String fileFilter = "glob:**/*.java";

		List<MatchLocation> results = contentSearcher.findMatches(applicationPath, fileFilter, regex);

		// Expect: 4 matches in com/todo/app/TaskController.java file
		assertThat(results).hasSize(4);

		assertThat(results).extracting(MatchLocation::lineNumber).containsExactly(27, 32, 37, 43);

		// Verify the matched text is exactly the pattern
		assertThat(results).extracting(MatchLocation::matchedText).containsOnly("@GetMapping");

		// Verify the file path is correct
		assertThat(results.get(0).filePath().getFileName().toString()).isEqualTo("TaskController.java");
	}

	@Test
	@DisplayName("Should return empty list if file filter finds no files")
	void testContentSearch_NoFilesFound() {
		String regex = ".*";
		String fileFilter = "glob:**/missing.xml";

		List<MatchLocation> results = contentSearcher.findMatches(applicationPath, fileFilter, regex);
		assertThat(results).isEmpty();
	}
}