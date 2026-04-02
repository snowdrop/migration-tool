package dev.snowdrop.langchain4j.vertexai;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@TopCommand
@Command(name = "code", mixinStandardHelpOptions = true)
public class CodeAssistantCommand implements Runnable {

	@Parameters(defaultValue = "Java Hello World class", description = "The tasks to be executed by the AI coding assistant")
	String task;

	@Inject
	CodeAssistantService codeAssistantService;

	@Override
	public void run() {
		System.out.println(codeAssistantService.writeCode(task));
	}
}