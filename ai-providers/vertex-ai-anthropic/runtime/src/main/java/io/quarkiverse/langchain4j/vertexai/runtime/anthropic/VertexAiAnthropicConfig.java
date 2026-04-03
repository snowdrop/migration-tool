package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.ToolChoice;

import java.util.List;

public class VertexAiAnthropicConfig {

    // Common LLM parameters as documented by Langchain4j here
    // https://github.com/langchain4j/langchain4j/blob/main/langchain4j-core/src/main/java/dev/langchain4j/model/chat/request/ChatRequestParameters.java#L13
    final Double temperature;
    final Double topP;
    final Integer topK;
    final Integer maxOutputTokens;
    final List<String> stopSequences;
    final List<ToolSpecification> toolSpecifications;
    final ToolChoice toolChoice;
    // See: https://platform.claude.com/docs/en/api/messages#tool.strict
    // When true, guarantees schema validation on tool names and inputs
    final Boolean strict;
    final boolean includeThoughts;
    Integer thinkingBudgetTokens;
    String thinkingType;

    public VertexAiAnthropicConfig(Builder builder) {
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.maxOutputTokens = builder.maxOutputTokens;
        this.stopSequences = builder.stopSequences;
        this.toolSpecifications = builder.toolSpecifications;
        this.toolChoice = builder.toolChoice;
        this.strict = builder.strict;
        this.includeThoughts = builder.includeThoughts;
        this.thinkingBudgetTokens = builder.thinkingBudgetTokens;
        this.thinkingType = builder.thinkingType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxOutputTokens;
        private List<String> stopSequences;
        private List<ToolSpecification> toolSpecifications;
        private ToolChoice toolChoice;
        private Boolean strict;
        private Boolean includeThoughts;
        private Integer thinkingBudgetTokens;
        private String thinkingType = "enabled";

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder maxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public Builder toolSpecifications(List<ToolSpecification> toolSpecifications) {
            this.toolSpecifications = toolSpecifications;
            return this;
        }

        public Builder toolChoice(ToolChoice toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder strict(Boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder includeThoughts(Boolean includeThoughts) {
            this.includeThoughts = includeThoughts;
            return this;
        }

        public Builder thinkingBudgetTokens(Integer thinkingBudgetTokens) {
            this.thinkingBudgetTokens = thinkingBudgetTokens;
            return this;
        }

        public Builder thinkingType(String thinkingType) {
            this.thinkingType = thinkingType;
            return this;
        }

        public VertexAiAnthropicConfig build() {
            return new VertexAiAnthropicConfig(this);
        }
    }
}