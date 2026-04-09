package dev.snowdrop.ai.workflow;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "flow", mixinStandardHelpOptions = true)
public class AiWorkFlowCommand implements Runnable {

    @Inject
    StoryCreatorFlow storyCreatorFlow;

    @Override
    public void run() {
        //System.out.println("Chatting ..." + aiModel.chat("Tell me what Brussels is in 2 lines"));
        System.out.println("Creating a story ...");
        String story = storyCreatorFlow.write("Tell me a story about Brussels !");
        System.out.println(story);
        System.out.println("-".repeat(90));
    }
}
