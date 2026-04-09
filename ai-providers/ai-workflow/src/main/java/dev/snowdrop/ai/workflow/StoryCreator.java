package dev.snowdrop.ai.workflow;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

public interface StoryCreator {
    @SequenceAgent(outputKey = "story", subAgents = { CreativeWriter.class })
    String write(@V("topic") String topic);
}