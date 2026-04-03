package io.quarkiverse.langchain4j.vertexai.runtime.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {

    @JsonProperty("system")
    SYSTEM,
    @JsonProperty("user")
    USER,
    @JsonProperty("assistant")
    ASSISTANT,
    @JsonProperty("tool")
    TOOL
}