package dev.snowdrop.mtool.tests.analyze;

import dev.snowdrop.mtool.analyze.services.ResultsService;
import dev.snowdrop.mtool.model.analyze.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsServiceTest {

    @BeforeAll
    static void initConfig() {
        System.setProperty("analyzer.rules.repo_url", "https://example.com/rules/%s.yaml");
    }

    // -----------------------------------------------------------------
    // generateScanDataTable
    // -----------------------------------------------------------------

    @Test
    void generateScanDataTableEmptyResults() {
        ResultsService service = new ResultsService();
        Map<String, List<Result>> results = Map.of("java:annotation", List.of());

        List<String[]> table = service.generateScanDataTable(results);

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[0]).isEqualTo("java:annotation");
        assertThat(table.get(0)[1]).isEqualTo("No match found");
    }

    @Test
    void generateScanDataTableNullValueTreatedAsEmpty() {
        ResultsService service = new ResultsService();
        Map<String, List<Result>> results = new LinkedHashMap<>();
        results.put("html:", null);

        List<String[]> table = service.generateScanDataTable(results);

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[1]).isEqualTo("No match found");
    }

    @Test
    void generateScanDataTableSingleTreeSitterResult() {
        ResultsService service = new ResultsService();
        String resultText = "Path: src/main/java/Foo.java, start: (1, 1), end: (10-1), text: @Controller";
        Result r = new Result("java-annotation", "treesitter", resultText);

        List<String[]> table = service.generateScanDataTable(Map.of("java:annotation", List.of(r)));

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[0]).isEqualTo("java:annotation");
        assertThat(table.get(0)[1]).isEqualTo(resultText);
    }

    @Test
    void generateScanDataTableMultipleResultsJoinedWithSeparator() {
        ResultsService service = new ResultsService();
        Result r1 = new Result("dep-1", "maven", "pom.xml:5 | org.springframework:spring-web");
        Result r2 = new Result("dep-2", "maven", "pom.xml:12 | org.springframework:spring-core");

        List<String[]> table = service.generateScanDataTable(Map.of("pom:dependency", List.of(r1, r2)));

        assertThat(table).hasSize(1);
        String details = table.get(0)[1];
        assertThat(details).contains("pom.xml:5 | org.springframework:spring-web");
        assertThat(details).contains("pom.xml:12 | org.springframework:spring-core");
        assertThat(details).contains("\n--- result ---\n");
    }

    @Test
    void generateScanDataTableSortedByQueryKey() {
        ResultsService service = new ResultsService();
        Map<String, List<Result>> results = new LinkedHashMap<>();
        results.put("pom:dependency", List.of(new Result("d1", "maven", "dep-result")));
        results.put("java:annotation", List.of(new Result("a1", "treesitter", "annotation-result")));
        results.put("html:", List.of(new Result("h1", "treesitter", "html-result")));

        List<String[]> table = service.generateScanDataTable(results);

        assertThat(table).hasSize(3);
        assertThat(table.get(0)[0]).isEqualTo("html:");
        assertThat(table.get(1)[0]).isEqualTo("java:annotation");
        assertThat(table.get(2)[0]).isEqualTo("pom:dependency");
    }

    // -----------------------------------------------------------------
    // generateDataTable (Map<String, List<Result>> overload)
    // -----------------------------------------------------------------

    @Test
    void generateDataTableWithoutSourceNorTargetProducesEmptyMigrationColumn() {
        ResultsService service = new ResultsService();
        Result r = new Result("a1", "treesitter", "some result");

        List<String[]> table = service.generateDataTable(Map.of("rule-1", List.of(r)));

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[0]).isEqualTo("rule-1");
        assertThat(table.get(0)[1]).isEmpty();
        assertThat(table.get(0)[2]).isEqualTo("Yes");
        assertThat(table.get(0)[3]).isEqualTo("some result");
    }

    @Test
    void generateDataTableWithSourceAndTargetFormatsCorrectly() {
        ResultsService service = new ResultsService("springboot", "quarkus");
        Result r = new Result("a1", "treesitter", "some result");

        List<String[]> table = service.generateDataTable(Map.of("rule-1", List.of(r)));

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[1]).isEqualTo("springboot -> quarkus");
    }

    @Test
    void generateDataTableEmptyResultsShowsNoMatch() {
        ResultsService service = new ResultsService("springboot", "quarkus");

        List<String[]> table = service.generateDataTable(Map.of("rule-1", List.of()));

        assertThat(table).hasSize(1);
        assertThat(table.get(0)[2]).isEqualTo("No");
        assertThat(table.get(0)[3]).isEqualTo("No match found");
    }
}
