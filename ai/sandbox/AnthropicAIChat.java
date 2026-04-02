///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j:1.12.2
//DEPS dev.langchain4j:langchain4j-anthropic:1.12.2
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.log.dev.langchain4j=info

import dev.langchain4j.model.anthropic.AnthropicChatModel;

public class AnthropicAIChat {
	public static void main(String[] args) {
		AnthropicChatModel model = AnthropicChatModel.builder()
				.apiKey(System.getenv("API_KEY"))
				.baseUrl("https://prod.ibm-bob-staging.cloud.ibm.com/v1")
				.modelName("premium")
				.build();
		String answer = model.chat("Hi. Can you tell me what Quarkus is about ?");
		System.out.println(answer);
	}
}
