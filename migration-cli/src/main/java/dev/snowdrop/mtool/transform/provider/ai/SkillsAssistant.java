package dev.snowdrop.mtool.transform.provider.ai;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface SkillsAssistant {
    @ToolBox(FileSystemTool.class)
    String chat(String message);
}
