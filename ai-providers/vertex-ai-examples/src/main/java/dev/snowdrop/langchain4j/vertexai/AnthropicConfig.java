package dev.snowdrop.langchain4j.vertexai;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.langchain4j.vertexai.anthropic")
public interface AnthropicConfig {

    // These will be mapped from:
    // QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID
    String projectId();

    // QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION
    String location();

    // QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_MODEL_ID
    @WithDefault("claude-opus-4-6")
    String modelId();
}