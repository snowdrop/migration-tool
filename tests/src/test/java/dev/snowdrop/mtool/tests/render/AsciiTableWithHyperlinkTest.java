package dev.snowdrop.mtool.tests.render;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import dev.snowdrop.mtool.analyze.reporting.ResultsService;
import dev.snowdrop.mtool.formatting.TerminalStyles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AsciiTableWithHyperlinkTest {

    @Test
    void testCreateHyperlink() {
        String link = TerminalStyles.createLink("https://google.com", "Google me");
        assertNotNull(link);
        assertFalse(link.isEmpty());
    }

    @Test
    void testAsciiTableWithStyler() {
        List<String[]> tableData = new ArrayList<>();
        tableData.add(new String[] {
                "000-springboot-annotation-notfound", "springboot -> quarkus", "No", "No match found" });
        tableData.add(new String[] {
                "001-springboot-replace-bom-quarkus", "springboot -> quarkus", "Yes",
                "Found SpringBootApplication at line 6" });
        tableData.add(new String[] {
                "002-springboot-add-class-quarkus", "springboot -> quarkus", "Yes",
                "Found SpringBootApplication at line 6" });
        tableData.add(new String[] {
                "003-springboot-to-quarkusmain-annotation", "springboot -> quarkus", "Yes",
                "Found SpringBootApplication at line 6" });
        tableData.add(new String[] {
                "004-springboot-to-quarkus-rest-annotations", "springboot -> quarkus", "Yes",
                "Found PostMapping at line 77" });

        String asciiTable = AsciiTable.builder()
                .styler(TerminalStyles.customizeStyle(ResultsService.RULE_REPO_URL_FORMAT))
                .data(tableData,
                        Arrays.asList(
                                new Column().header("Rule ID").headerAlign(HorizontalAlign.LEFT)
                                        .dataAlign(HorizontalAlign.LEFT).with(r -> r[0]),
                                new Column().header("Source to Target").headerAlign(HorizontalAlign.LEFT)
                                        .dataAlign(HorizontalAlign.LEFT).with(r -> r[1]),
                                new Column().header("Match").headerAlign(HorizontalAlign.CENTER)
                                        .dataAlign(HorizontalAlign.CENTER).with(r -> r[2]),
                                new Column().header("Information Details").headerAlign(HorizontalAlign.LEFT)
                                        .maxWidth(120).dataAlign(HorizontalAlign.LEFT).with(r -> r[3])))
                .asString();

        assertNotNull(asciiTable);
        assertFalse(asciiTable.isEmpty());
    }
}