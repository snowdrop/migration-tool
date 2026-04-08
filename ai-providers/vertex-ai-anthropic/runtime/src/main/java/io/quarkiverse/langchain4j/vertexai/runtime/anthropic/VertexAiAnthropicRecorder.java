package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import static io.quarkiverse.langchain4j.runtime.OptionalUtil.firstOrDefault;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.config.VertexAiAnthropicConfig;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.ConfigValidationException;

@Recorder
public class VertexAiAnthropicRecorder {

    private static final TypeLiteral<Instance<ChatModelListener>> CHAT_MODEL_LISTENER_TYPE_LITERAL = new TypeLiteral<>() {
    };

    private final RuntimeValue<VertexAiAnthropicConfig> runtimeConfig;

    public VertexAiAnthropicRecorder(RuntimeValue<VertexAiAnthropicConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Function<SyntheticCreationalContext<ChatModel>, ChatModel> chatModel(String configName) {
        var aiConfig = getDefaultOrNamedConfig(configName);

        if (aiConfig.enableIntegration()) {
            var chatModelConfig = aiConfig.chatModel();

            // The base-url is calculated according to the location provided: https://<location>-aiplatform.googleapis.com
            // and should not be provided except for testing purposes
            Optional<String> baseUrl = aiConfig.baseUrl();

            String location = aiConfig.location();
            if (location.isEmpty()) {
                throw new ConfigValidationException(createConfigProblems("location", configName));
            }

            String projectId = aiConfig.projectId();
            if (projectId.isEmpty()) {
                throw new ConfigValidationException(createConfigProblems("project-id", configName));
            }

            String modelId = aiConfig.modelId();
            if (modelId.isEmpty()) {
                throw new ConfigValidationException(createConfigProblems("model-id", configName));
            }

            var builder = VertexAiAnthropicChatModel.builder()
                    .baseUrl(baseUrl)
                    .projectId(projectId)
                    .location(location)
                    .publisher(aiConfig.publisher())
                    .modelId(modelId)
                    .logRequests(firstOrDefault(false, aiConfig.logRequests()))
                    .logResponses(firstOrDefault(false, aiConfig.logResponses()))
                    .logCurl(firstOrDefault(false, aiConfig.logRequestsCurl()));

            if (aiConfig.timeout().isPresent()) {
                builder.timeout(aiConfig.timeout().get());
            }

            if (chatModelConfig.temperature().isPresent()) {
                builder.temperature(chatModelConfig.temperature().getAsDouble());
            }

            if (chatModelConfig.topP().isPresent()) {
                builder.topP(chatModelConfig.topP().getAsDouble());
            }

            if (chatModelConfig.topK().isPresent()) {
                builder.topK(chatModelConfig.topK().getAsInt());
            }

            builder.maxOutputTokens(chatModelConfig.maxOutputTokens());

            if (chatModelConfig.thinking().includeThoughts()) {
                builder.includeThoughts(true);
                builder.thinkingBudgetTokens(chatModelConfig.thinking().thinkingBudgetTokens().get());
                builder.thinkingType(chatModelConfig.thinking().thinkingType());
            }

            return new Function<>() {
                @Override
                public ChatModel apply(SyntheticCreationalContext<ChatModel> context) {
                    builder.listeners(context.getInjectedReference(CHAT_MODEL_LISTENER_TYPE_LITERAL).stream()
                            .collect(Collectors.toList()));
                    return builder.build();
                }
            };
        } else {
            return new Function<>() {
                @Override
                public ChatModel apply(SyntheticCreationalContext<ChatModel> context) {
                    return new DisabledChatModel();
                }
            };
        }
    }

    private VertexAiAnthropicConfig.VertexAiConfig getDefaultOrNamedConfig(String configName) {
        return NamedConfigUtil.isDefault(configName) ? runtimeConfig.getValue().defaultConfig()
                : runtimeConfig.getValue().namedConfig().get(configName);
    }

    private static ConfigValidationException.Problem[] createConfigProblems(String key, String configName) {
        return new ConfigValidationException.Problem[] { createConfigProblem(key, configName) };
    }

    private static ConfigValidationException.Problem createConfigProblem(String key, String configName) {
        return new ConfigValidationException.Problem(
                "SRCFG00014: The config property quarkus.langchain4j.vertexai%s%s is required but it could not be found in any config source"
                        .formatted(
                                NamedConfigUtil.isDefault(configName) ? "." : ("." + configName + "."), key));
    }
}
