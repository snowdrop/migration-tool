package dev.snowdrop.mtool.tests.scanner.file;

import dev.snowdrop.mtool.model.analyze.FilePath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileSearchTest extends AbstractFileSearchTest {

	@Test
	@DisplayName("Should find all application.* files")
	void testPropertiesFilesSearch() {

		List<FilePath> results = fileSearcher.findPropertiesPaths(applicationPath, "properties");

		// Expect: application.properties
		assertThat(results).hasSize(1);
		assertThat(results).extracting(path -> applicationPath.relativize(path.filePath()).toString())
				.containsExactlyInAnyOrder("src/main/resources/application.properties");
	}

	@Test
	@DisplayName("Should find all Java files using REGEX")
	void testRegexSourceSearch() {
		String filesSearchPattern = "regex:.*\\.(java)$";

		List<FilePath> results = fileSearcher.findPaths(applicationPath, filesSearchPattern, false);

		assertThat(results).hasSize(7);
		assertThat(results).extracting(path -> applicationPath.relativize(path.filePath()).toString())
				.containsExactlyInAnyOrder("src/test/java/com/todo/app/AppApplicationTests.java",
						"src/main/java/com/todo/app/repository/TaskRepository.java",
						"src/main/java/com/todo/app/entity/Task.java",
						"src/main/java/com/todo/app/controller/TaskController.java",
						"src/main/java/com/todo/app/AppApplication.java",
						"src/main/java/com/todo/app/service/TaskService.java",
						"src/main/java/com/todo/app/service/TaskServiceImpl.java");
	}

	@Test
	@DisplayName("Should not find Dockerfile")
	void testGlobDockerfileSearch() {
		String filesSearchPattern = "glob:**/Dockerfile*";

		List<FilePath> results = fileSearcher.findPaths(applicationPath, filesSearchPattern, false);
		assertThat(results).hasSize(0);
	}

	@Test
	@DisplayName("Should find src/main/resources directory structure using GLOB")
	void testGlobDirectorySearch() {
		String filesSearchPattern = "glob:src/main/resources";

		List<FilePath> results = fileSearcher.findPaths(applicationPath, filesSearchPattern, true);

		// Expect: src/main/resources (1 directory)
		assertThat(results).hasSize(1);
		assertThat(results.get(0).isDirectory()).isTrue();
		assertThat(applicationPath.relativize(results.get(0).filePath()).toString()).isEqualTo("src/main/resources");
	}
}