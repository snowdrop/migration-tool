package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import static io.quarkiverse.langchain4j.runtime.LangChain4jUtil.chatMessageToText;

import java.util.*;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCacheType;
import dev.langchain4j.model.anthropic.internal.api.AnthropicThinking;
import dev.langchain4j.model.anthropic.internal.api.AnthropicTool;
import dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper;

/**
 * Generate the Request using the list of the Chat Messages
 */
public class ContentMapper {
    private final static String ANTHROPIC_VERSION = "vertex-2023-10-16";

    /**
     * Generate the Request from the list of the chat messages and VertexAiconfig
     * The request structure has been designed according to the Claude API spec:
     * <a href="https://platform.claude.com/docs/en/api/java/messages/create">Claude API</a>
     *
     * @param messages the Chat Messages
     * @param vertexAiConfig
     * @return the GenerateRequest
     */
    public static GenerateRequest map(
            List<ChatMessage> messages,
            VertexAiAnthropicConfig vertexAiConfig) {

        // Create the AnthropicThinking if the user requested to see the thoughts !
        AnthropicThinking anthropicThinking = null;
        if (vertexAiConfig.includeThoughts) {
            if (vertexAiConfig.thinkingType != null || vertexAiConfig.thinkingBudgetTokens != null) {
                anthropicThinking = AnthropicThinking.builder()
                        .type(vertexAiConfig.thinkingType)
                        .budgetTokens(vertexAiConfig.thinkingBudgetTokens)
                        .build();
            }
        }

        // Create the ToolSpecifications
        List<ToolSpecification> toolSpecifications = List.of();
        if (vertexAiConfig.toolSpecifications != null && !vertexAiConfig.toolSpecifications.isEmpty()) {
            toolSpecifications = vertexAiConfig.toolSpecifications;
        }

        List<GenerateRequest.Message> requestMessages = new ArrayList<>();
        String systemMessage = "";

        for (ChatMessage message : messages) {
            switch (message.type()) {
                case SYSTEM -> systemMessage = chatMessageToText(message);
                case USER -> requestMessages
                        .add(new GenerateRequest.Message(Role.USER.name().toLowerCase(), chatMessageToText(message)));
                case AI -> requestMessages
                        .add(new GenerateRequest.Message(Role.ASSISTANT.name().toLowerCase(), chatMessageToText(message)));

                // As Anthropic API only accepts as role: user and assistant, then we return user
                // We cannot use as role assistant as we will get as response:
                // This model does not support assistant message prefill. The conversation must end with a user message.
                case TOOL_EXECUTION_RESULT ->
                    requestMessages
                            .add(new GenerateRequest.Message(Role.USER.name().toLowerCase(), chatMessageToText(message)));
                default -> throw new IllegalArgumentException("Unsupported message type: " + message.type());
            }
        }

        return new GenerateRequest(
                ANTHROPIC_VERSION,
                vertexAiConfig.maxOutputTokens,
                requestMessages,
                systemMessage,
                toTools(toolSpecifications, vertexAiConfig.strict),
                anthropicThinking);
    }

    public static List<AnthropicTool> toTools(List<ToolSpecification> toolSpecifications, boolean strict) {
        return AnthropicMapper.toAnthropicTools(toolSpecifications, AnthropicCacheType.NO_CACHE, strict);
    }
}
