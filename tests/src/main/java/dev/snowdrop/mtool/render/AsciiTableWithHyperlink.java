package dev.snowdrop.mtool.render;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import dev.snowdrop.mtool.analyze.services.ResultsService;
import dev.snowdrop.mtool.analyze.utils.TerminalUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: Convert it to a test case
public class AsciiTableWithHyperlink {

	final static String RULE_REPO_URL = "https://github.com/snowdrop/migration-tool/blob/main/cookbook/rules/quarkus/%s.yaml";
	final static String RESET = "\u001B[m";
	final static String GREEN = "\u001B[32m";

	static void main(String[] args) {

		System.out.println("==== Print an hyperlink using createLink method ======");
		System.out.println(TerminalUtils.createLink("https://google.com", "Google me"));

		List<String[]> tableData = new ArrayList<>();
		tableData.add(
				new String[]{"000-springboot-annotation-notfound", "springboot -> quarkus", "No", "No match found"});
		tableData.add(new String[]{"001-springboot-replace-bom-quarkus", "springboot -> quarkus", "Yes",
				"Found SpringBootApplication at line 6, char: 1 - 22\nfile:///Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/src/main/java/com/todo/app/AppApplication.java"});
		tableData.add(new String[]{"002-springboot-add-class-quarkus", "springboot -> quarkus", "Yes",
				"Found SpringBootApplication at line 6, char: 1 - 22\nfile:///Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/src/main/java/com/todo/app/AppApplication.java"});
		tableData.add(new String[]{"003-springboot-to-quarkusmain-annotation", "springboot -> quarkus", "Yes",
				"Found SpringBootApplication at line 6, char: 1 - 22\nfile:///Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/src/main/java/com/todo/app/AppApplication.java"});
		tableData.add(new String[]{"004-springboot-to-quarkus-rest-annotations", "springboot -> quarkus", "Yes",
				"Found PostMapping at line 77, char: 5 - 17\nfile:///Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/src/main/java/com/todo/app/controller/TaskController.java\n--- rewrite ---\nFound ResponseBody at line 77, char: 5 - 17\nfile:///Users/cmoullia/code/application-modernisation/migration-tool-parent/applications/demo-spring-boot-todo-app/src/main/java/com/todo/app/controller/TaskController.java"});

		System.out.println("\n=== Migration data with Styler ===");
		var asciiTable = AsciiTable.builder().styler(TerminalUtils.customizeStyle(ResultsService.RULE_REPO_URL_FORMAT))
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
		System.out.println(asciiTable);
	}
}