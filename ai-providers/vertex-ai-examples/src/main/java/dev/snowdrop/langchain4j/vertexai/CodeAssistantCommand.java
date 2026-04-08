package dev.snowdrop.langchain4j.vertexai;

import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.config.VertexAiAnthropicConfig;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.smallrye.config.ConfigValidationException;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@TopCommand
@Command(name = "code", mixinStandardHelpOptions = true)
public class CodeAssistantCommand implements Runnable {

    @Parameters(defaultValue = "Java Hello World class", description = "The tasks to be executed by the AI coding assistant")
    String task;

    @Inject
    CodeAssistantService codeAssistantService;

    @Inject
    VertexAiAnthropicConfig cfg;

    @Override
    public void run() {
        VertexAiAnthropicConfig.VertexAiConfig vertexCfg = cfg.defaultConfig();

        String location = vertexCfg.location();
        if (location.equals("dummy")) {
            throw new ConfigValidationException(createConfigProblems("location"));
        }

        String projectId = vertexCfg.projectId();
        if (projectId.equals("dummy")) {
            throw new ConfigValidationException(createConfigProblems("project-id"));
        }

        System.out.println("--- Configuration Loaded ---");
        System.out.println("Project ID: " + projectId);
        System.out.println("Location:   " + location);
        System.out.println("Model ID:    " + vertexCfg.modelId());

        System.out.println(codeAssistantService.writeCode(task));
    }

    private static ConfigValidationException.Problem[] createConfigProblems(String key) {
        return new ConfigValidationException.Problem[] { createConfigProblem(key) };
    }

    private static ConfigValidationException.Problem createConfigProblem(String key) {
        return new ConfigValidationException.Problem(
                "SRCFG00014: The config property: quarkus.langchain4j.vertexai.anthropic.%s is required but it could not be found as environment variable"
                        .formatted(key));
    }
}