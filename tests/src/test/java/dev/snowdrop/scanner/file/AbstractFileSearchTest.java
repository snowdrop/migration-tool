package dev.snowdrop.scanner.file;

import dev.snowdrop.scanner.file.search.FileSearch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for all files and content search tests.
 * Manages the creation and cleanup of the temporary mock project environment.
 */
public abstract class AbstractFileSearchTest {

	protected FileSearch fileSearcher;
	protected FileSearch contentSearcher;
	protected static Path applicationPath;

	@BeforeAll
	static void setUp() throws URISyntaxException {
		applicationPath = applicationPath("spring-boot-todo-app");
	}

	@BeforeEach
	void setup() throws IOException {
		fileSearcher = new FileSearch();
		contentSearcher = new FileSearch();
	}

	public static Path applicationPath(String appFolderName) throws URISyntaxException {
		URL resourceUrl = AbstractFileSearchTest.class.getClassLoader().getResource(appFolderName);
		if (resourceUrl == null) {
			throw new RuntimeException("Application folder not found: " + appFolderName);
		}
		return Paths.get(resourceUrl.toURI());
	}
}