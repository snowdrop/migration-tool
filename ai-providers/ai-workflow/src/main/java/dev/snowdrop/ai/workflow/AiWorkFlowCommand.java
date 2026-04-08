package dev.snowdrop.ai.workflow;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "flow", mixinStandardHelpOptions = true)
public class AiWorkFlowCommand implements Runnable {
    @Inject
    ChatModel aiModel;

    @Override
    public void run() {
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(aiModel)
                .outputKey("story")
                .build();

        creativeWriter.generateStory("Tell me a story about Brussels :-)");
    }
}
