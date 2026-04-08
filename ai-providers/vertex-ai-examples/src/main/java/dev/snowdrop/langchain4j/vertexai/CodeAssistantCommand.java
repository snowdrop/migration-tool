package dev.snowdrop.langchain4j.vertexai;

import io.quarkiverse.langchain4j.vertexai.runtime.anthropic.VertexAiAnthropicConfig;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
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

    @Override
    public void run() {
        try {
            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources() // This enables Environment Variables support!
                    .addDefaultInterceptors()
                    .withMapping(AnthropicConfig.class)
                    .build();

            AnthropicConfig aiCfg = config.getConfigMapping(AnthropicConfig.class);
            System.out.println("--- Configuration Loaded ---");
            System.out.println("Project ID: " + aiCfg.projectId());
            System.out.println("Location:   " + aiCfg.location());
            System.out.println("Model ID:    " + aiCfg.modelId());

        } catch (ConfigValidationException e) {
            System.err.println("Please set the following mandatory environment variables:");
            System.err.println(" - QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_PROJECT_ID");
            System.err.println(" - QUARKUS_LANGCHAIN4J_VERTEXAI_ANTHROPIC_LOCATION");
            System.exit(1);
        }
        System.out.println(codeAssistantService.writeCode(task));
    }
}