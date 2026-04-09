package dev.snowdrop.ai.workflow;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

public interface StoryCreatorFlow {

    @SequenceAgent(outputKey = "story", subAgents = {
            AgentCreativeWriter.class,
            NonAiAgentCreativeWriter.class
    })
    String write(@V("topic") String topic);
}