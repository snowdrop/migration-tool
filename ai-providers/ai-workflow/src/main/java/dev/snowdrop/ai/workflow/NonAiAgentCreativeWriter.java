package dev.snowdrop.ai.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.CreatedAware;

@CreatedAware
public class NonAiAgentCreativeWriter {

    @Agent(description = "Generates a non AI story based on the given topic", outputKey = "story")
    public static String generateNonAIStory(@V("topic") String topic) {
        return "This is my story created by a non AI agent !!";
    }
}
