package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import static dev.langchain4j.internal.Utils.getOrDefault;

import java.time.Duration;
import java.util.*;

import dev.langchain4j.model.chat.request.ToolChoice;
import org.jboss.logging.Logger;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.anthropic.internal.api.AnthropicThinking;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

public abstract class VertexAiBaseChatModel implements ChatModel {

    private static final Logger log = Logger.getLogger(VertexAiBaseChatModel.class);

    protected final Double temperature;
    protected final Double topP;
    protected final Integer topK;
    protected final Integer maxOutputTokens;
    protected final List<String> stopSequences;
    protected final List<ToolSpecification> toolSpecifications;
    protected final ToolChoice toolChoice;
    protected final Boolean logRequests;
    protected final Boolean logResponses;
    protected final Duration timeout;

    // See: https://platform.claude.com/docs/en/api/messages#tool.strict
    // When true, guarantees schema validation on tool names and inputs
    protected Boolean strict;
    protected final boolean includeThoughts;
    protected Integer thinkingBudgetTokens;
    protected String thinkingType = "enabled";

    public VertexAiBaseChatModel(
            Double temperature,
            Double topP,
            Integer topK,
            Integer maxOutputTokens,
            List<String> stopSequences,
            List<ToolSpecification> toolSpecifications,
            ToolChoice toolChoice,
            Boolean logRequests,
            Boolean logResponses,
            Boolean strict,
            Duration timeout,
            boolean includeThoughts,
            Integer thinkingBudgetTokens,
            String thinkingType) {
        this.temperature = temperature;
        this.topP = topP;
        this.topK = topK;
        this.maxOutputTokens = maxOutputTokens;
        this.stopSequences = stopSequences;
        this.toolSpecifications = toolSpecifications;
        this.toolChoice = toolChoice;
        this.logRequests = logRequests;
        this.logResponses = logResponses;
        this.strict = strict;
        this.timeout = timeout;
        this.includeThoughts = includeThoughts;
        this.thinkingBudgetTokens = thinkingBudgetTokens;
        this.thinkingType = thinkingType;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        ChatRequestParameters requestParameters = chatRequest.parameters();

        VertexAiAnthropicConfig vertexAiConfig = VertexAiAnthropicConfig.builder()
                .temperature(getOrDefault(this.temperature, requestParameters.temperature()))
                .topP(getOrDefault(this.topP, requestParameters.topP()))
                .topK(getOrDefault(this.topK, requestParameters.topK()))
                .maxOutputTokens(getOrDefault(this.maxOutputTokens, requestParameters.maxOutputTokens()))
                .stopSequences(getOrDefault(this.stopSequences, requestParameters.stopSequences()))
                .toolSpecifications(getOrDefault(chatRequest.toolSpecifications(), this.toolSpecifications))
                .toolChoice(getOrDefault(this.toolChoice, chatRequest.toolChoice()))
                .strict(getOrDefault(this.strict, false))
                .includeThoughts(getOrDefault(this.includeThoughts, false))
                .build();

        if (logRequests) {
            log.info("Request: " + chatRequest.messages());
        }

        GenerateRequest request = ContentMapper.map(
                chatRequest.messages(),
                vertexAiConfig);
        GenerateResponse response = callApi(request);

        // Let's analyze the response we got to determine what AI is saying
        String aiText = GenerateResponseHandler.getText(response);
        List<ToolExecutionRequest> toolExecutionRequests = GenerateResponseHandler.getToolExecutionRequests(response);

        // If we have enabled thinking, then got the responses from LLM
        String thoughts = this.includeThoughts ? GenerateResponseHandler
                .getThoughts(response) : null;

        AiMessage.Builder aiMessageBuilder = AiMessage.builder()
                .text(aiText)
                .thinking(thoughts)
                .toolExecutionRequests(toolExecutionRequests);
        AiMessage aiMessage = aiMessageBuilder.build();

        ChatResponse chatResponse = ChatResponse.builder()
                .aiMessage(aiMessage)
                .finishReason(GenerateResponseHandler.getFinishReason(response))
                .build();

        if (logResponses) {
            log.info("Response: " + chatResponse);
        }

        return chatResponse;
    }

    abstract GenerateResponse callApi(GenerateRequest request);
}
