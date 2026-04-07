package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.ToolChoice;
import org.jboss.resteasy.reactive.client.api.LoggingScope;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

public class VertexAiAnthropicChatModel extends VertexAiBaseChatModel {

    private final VertexAiRestApi.ApiMetadata apiMetadata;
    private final VertexAiRestApi restApi;

    public VertexAiAnthropicChatModel(Builder builder) {
        super(builder.temperature,
                builder.topP,
                builder.topK,
                builder.maxOutputTokens,
                builder.stopSequences,
                builder.toolSpecifications,
                builder.toolChoice,
                builder.logRequests,
                builder.logResponses,
                builder.logCurl,
                builder.strict,
                builder.timeout,
                builder.includeThoughts,
                builder.thinkingBudgetTokens,
                builder.thinkingType);

        this.apiMetadata = VertexAiRestApi.ApiMetadata
                .builder()
                .modelId(builder.modelId)
                .location(builder.location)
                .projectId(builder.projectId)
                .publisher(builder.publisher)
                .build();
        try {
            String baseUrl = builder.baseUrl.orElse(String.format("https://%s-aiplatform.googleapis.com", builder.location));
            var restApiBuilder = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(new URI(baseUrl))
                    .connectTimeout(builder.timeout.toSeconds(), TimeUnit.SECONDS)
                    .readTimeout(builder.timeout.toSeconds(), TimeUnit.SECONDS);

            if (builder.logRequests || builder.logResponses || builder.logCurl) {
                restApiBuilder.loggingScope(LoggingScope.REQUEST_RESPONSE);
                restApiBuilder.clientLogger(new VertexAiRestApi.VertxAiClientLogger(
                        builder.logRequests,
                        builder.logResponses,
                        builder.logCurl));
            }
            restApiBuilder.register(new ModelAuthProviderFilter(builder.modelId));
            restApi = restApiBuilder.build(VertexAiRestApi.class);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected GenerateResponse callApi(GenerateRequest request) {
        return restApi.createMessage(request, apiMetadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Optional<String> baseUrl = Optional.empty();
        private String projectId;
        private String location;
        private String modelId;
        private String publisher;

        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxOutputTokens;
        public List<String> stopSequences;
        public List<ToolSpecification> toolSpecifications;
        public ToolChoice toolChoice;

        private Duration timeout = Duration.ofSeconds(10);
        private Boolean logRequests = false;
        private Boolean logResponses = false;
        private Boolean logCurl = false;
        private List<ChatModelListener> listeners = Collections.emptyList();
        private Boolean strict = false;

        private Boolean includeThoughts = false;
        private Integer thinkingBudgetTokens;
        private String thinkingType;

        public Builder baseUrl(Optional<String> baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

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

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public Builder logCurl(boolean logCurl) {
            this.logCurl = logCurl;
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder includeThoughts(boolean includeThoughts) {
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

        public Builder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public VertexAiAnthropicChatModel build() {
            return new VertexAiAnthropicChatModel(this);
        }
    }
}
