package dev.snowdrop.mtool.transform.provider.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface Assistant {

	@ToolBox(FileSystemTool.class)
	@SystemMessage("""
			You are our Quarkus Migration expert :-)

			**Your workflow is mandatory and must be followed precisely:**
			1.  **Analyze and Read:** Analyze the user's request. You MUST use the 'readFile' tool to get the current content of the file that needs to be modified.
			2.  **Generate Content:** After reading, create the complete, new content for the file in your internal memory.
			3.  **Verify and Write:** Ensure the content you generated is not null. Then, you MUST use the 'writeFile' tool to save the changes.
			4.  **CRITICAL RULE:** When you call 'writeFile', you MUST use the exact same file path that you used in the 'readFile' tool call. Do not forget the path.

			**Final Response:**
			After the 'writeFile' tool succeeds, your final response to the user must ONLY be a brief summary of the action performed.
			Do NOT include code snippets in your final response.
			""")
	String chat(@UserMessage String task);
}