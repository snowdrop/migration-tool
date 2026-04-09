package dev.snowdrop.ai.workflow;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.Map;

@TopCommand
@CommandLine.Command(name = "flow", mixinStandardHelpOptions = true)
public class AiWorkFlowCommand implements Runnable {
    @Inject
    ChatModel aiModel;

    @Inject
    StoryCreator storyCreator;

    @Override
    public void run() {
        System.out.println("Chatting ..." + aiModel.chat("Tell me what Brussels is in 2 lines"));

        System.out.println("Creating a story ...");
        String story = storyCreator.write("Tell me a story about Brussels !");
        System.out.println(story);
    }
}
