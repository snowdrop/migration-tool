package dev.snowdrop.mapper.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class LoadParseMappingYamlFileTest {

	private final static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
	private final static String CONFIG_FILE = "query-scanner-mapping.yml";
	private static String YAML_CONFIG;

	@BeforeAll
	static void init() throws Exception {
		YAML_CONFIG = Files.readString(
				Paths.get(LoadParseMappingYamlFileTest.class.getClassLoader().getResource(CONFIG_FILE).toURI()));
	}

	@Test
	public void shouldHaveQueryScannerMapping() throws JsonProcessingException {
		QueryScannerMapping queryScannerMapping = MAPPER.readValue(YAML_CONFIG, QueryScannerMapping.class);
		Assertions.assertNotNull(queryScannerMapping);
	}

	@Test
	public void shouldHaveJavaTypeMap() throws JsonProcessingException {
		QueryScannerMapping queryScannerMapping = MAPPER.readValue(YAML_CONFIG, QueryScannerMapping.class);
		Assertions.assertNotNull(queryScannerMapping);

		Map<String, ScannerConfig> javaType = queryScannerMapping.getJavaQueries();
		Assertions.assertNotNull(javaType);
	}

	@Test
	public void shouldHaveJavaScannerConfig() throws JsonProcessingException {
		QueryScannerMapping queryScannerMapping = MAPPER.readValue(YAML_CONFIG, QueryScannerMapping.class);
		Assertions.assertNotNull(queryScannerMapping);

		Map<String, ScannerConfig> javaType = queryScannerMapping.getJavaQueries();
		Assertions.assertNotNull(javaType);

		ScannerConfig annotationSymbol = javaType.get("annotation");
		Assertions.assertNotNull(annotationSymbol);

		Assertions.assertEquals("openrewrite", annotationSymbol.getScanner());
		Assertions.assertEquals("dev.snowdrop.model.JavaAnnotationDTO", annotationSymbol.getDto());
	}

	@Test
	public void shouldHavePomScannerConfig() throws JsonProcessingException {
		QueryScannerMapping queryScannerMapping = MAPPER.readValue(YAML_CONFIG, QueryScannerMapping.class);
		Assertions.assertNotNull(queryScannerMapping);

		Map<String, ScannerConfig> pomType = queryScannerMapping.getPomQueries();
		Assertions.assertNotNull(pomType);

		ScannerConfig dependencySymbol = pomType.get("dependency");
		Assertions.assertNotNull(dependencySymbol);

		Assertions.assertEquals("maven", dependencySymbol.getScanner());
		Assertions.assertEquals("dev.snowdrop.model.MavenDependencyDTO", dependencySymbol.getDto());
	}

}
