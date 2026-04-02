///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.12.2
//DEPS dev.langchain4j:langchain4j-anthropic:1.12.2
//DEPS dev.langchain4j:langchain4j-vertex-ai-anthropic:1.12.2-beta22
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=debug

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.vertexai.anthropic.VertexAiAnthropicChatModel;

public class VertexAIAnthropicChat {

	private static final String PROJECT_ID = "itpc-gcp-cp-pe-eng-claude";
	private static final String MODEL_NAME = "claude-opus-4-6";
	private static final String LOCATION = "europe-west1";

	public static void main(String[] args) {
		System.out.println("Starting AI chat ...");
		ChatModel model = VertexAiAnthropicChatModel.builder().project(PROJECT_ID).location(LOCATION)
				.modelName(MODEL_NAME).maxTokens(1000).logRequests(true).logResponses(true).build();

		System.out.println(model.chat("Hi Claude"));
	}
}