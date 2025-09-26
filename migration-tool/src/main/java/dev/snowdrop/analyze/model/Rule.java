package dev.snowdrop.analyze.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Rule(
    String category,
    @JsonProperty("customVariables") List<String> customVariables,
    String description,
    int effort,
    List<String> labels,
    List<String> links,
    String message,
    String ruleID,
    String lsCmd,
    When when,
    @Deprecated @JsonProperty("actions") List<String> actions,
    List<String> instructions
) {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record When(
        @JsonProperty("java.referenced")
        JavaReferenced javaReferenced,
        @JsonProperty("or")
        List<Condition> or,
        @JsonProperty("and")
        List<Condition> and
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Condition(
        @JsonProperty("java.dependency")
        JavaDependency javaDependency,

        @JsonProperty("java.referenced")
        JavaReferenced javaReferenced,
        String as,
        String from,
        String not,
        String ignore
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record JavaReferenced(
        String location,
        String pattern,
        String filepaths,
        String annotated
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Annotated(
        String pattern,
        List<Element> elements
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Element(
        String name,
        String value
    ) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record JavaDependency(
        String lowerbound,
        String upperbound,
        String name,
        String nameregex
    ) {}
}