package dev.snowdrop.mtool.transform.provider.ai;

import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.skills.SkillsSystemMessageProvider;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(systemMessageProviderSupplier = SkillsSystemMessageProvider.class)
@ApplicationScoped
public interface SkillsAssistant {
    String chat(String message);
}
