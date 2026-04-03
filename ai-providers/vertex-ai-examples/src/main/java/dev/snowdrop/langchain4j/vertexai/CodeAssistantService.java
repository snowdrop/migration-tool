package dev.snowdrop.langchain4j.vertexai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface CodeAssistantService {

    // We got an error HTTP 400: Unexpected role "system". The Messages API accepts a top-level `system` parameter,
    // not "system" as an input message role."
    // See: https://platform.claude.com/docs/en/api/messages#message_param
    //@SystemMessage("You are a professional poet")

    @ToolBox(FileSystemTool.class)
    String writeCode(@UserMessage String task);
}
