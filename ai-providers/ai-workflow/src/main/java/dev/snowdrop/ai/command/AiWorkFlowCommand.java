package dev.snowdrop.ai.command;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.snowdrop.ai.workflow.AgentCreativeWriter;
import dev.snowdrop.ai.workflow.NonAiAgentCreativeWriter;
import dev.snowdrop.ai.workflow.StoryCreatorFlow;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.Map;

import static dev.langchain4j.agentic.AgenticServices.createAgenticSystem;

@TopCommand
@CommandLine.Command(name = "flow", mixinStandardHelpOptions = true)
public class AiWorkFlowCommand implements Runnable {

    @Inject
    ChatModel aiModel;

    @Inject
    StoryCreatorFlow storyCreatorFlow;

    @Override
    public void run() {
        System.out.println("Creating a story ...");

        // That works, but we don't get the string of the Non AI agent executed !!
        System.out.println(flowCreatedUsingAnnotations());

        /*
         * System.out.println(flowCreatedUsingFluentApi());
         *
         * Fails:
         * java.lang.IncompatibleClassChangeError: class dev.snowdrop.ai.workflow.NonAiAgentCreativeWriter$$QuarkusImpl can not
         * implement dev.snowdrop.ai.workflow.NonAiAgentCreativeWriter, because it is not an interface
         * (dev.snowdrop.ai.workflow.NonAiAgentCreativeWriter is in unnamed module of loader io.q
         * at java.base/java.lang.ClassLoader.defineClass1(Native Method)
         * at java.base/java.lang.ClassLoader.defineClass(ClassLoader.java:962)
         * at io.quarkus.bootstrap.classloading.QuarkusClassLoader.loadClass(QuarkusClassLoader.java:568)
         * at io.quarkus.bootstrap.classloading.QuarkusClassLoader.loadClass(QuarkusClassLoader.java:526)
         * at java.base/java.lang.Class.forName0(Native Method)
         * at java.base/java.lang.Class.forName(Class.java:547)
         * at io.quarkiverse.langchain4j.QuarkusAiServicesFactory$QuarkusAiServices.build(QuarkusAiServicesFactory.java:106)
         * at dev.langchain4j.agentic.agent.AgentBuilder.build(AgentBuilder.java:214)
         * at dev.langchain4j.agentic.agent.AgentBuilder.build(AgentBuilder.java:138)
         * at dev.snowdrop.ai.command.AiWorkFlowCommand.flowCreatedUsingFluentApi(AiWorkFlowCommand.java:51)
         */

        // That works like using the method: flowCreatedUsingAnnotations()
        //System.out.println(flowCreatedUsingAgenticSystem());

        System.out.println("-".repeat(90));
    }

    protected String flowCreatedUsingAnnotations() {
        return storyCreatorFlow.write("Tell me a story about Brussels !");
    }

    protected String flowCreatedUsingAgenticSystem() {
        StoryCreatorFlow storyWriter = createAgenticSystem(StoryCreatorFlow.class, aiModel);
        return storyWriter.write("Tell me a story about Brussels !");
    }

    protected String flowCreatedUsingFluentApi() {
        var creativeWriterAgentBuilder = AgenticServices.agentBuilder(AgentCreativeWriter.class)
                .chatModel(aiModel).outputKey("story").build();

        var nonAiCreativeWriterAgentBuilder = AgenticServices.agentBuilder(NonAiAgentCreativeWriter.class)
                .chatModel(aiModel).outputKey("story").build();

        UntypedAgent writerFlow = AgenticServices.sequenceBuilder()
                .subAgents(creativeWriterAgentBuilder, nonAiCreativeWriterAgentBuilder)
                .outputKey("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "Tell me a story about Brussels !");

        return (String) writerFlow.invoke(input);
    }
}
