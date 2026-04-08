///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.langchain4j:langchain4j-open-ai:1.12.2
//DEPS com.google.auth:google-auth-library-oauth2-http:1.43.0
//DEPS org.slf4j:slf4j-simple:2.0.17
import dev.langchain4j.model.openai.OpenAiChatModel;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;

public class VertexAIOpenAI {

    public static void main(String[] args) throws IOException {
        String projectId = "dummy";
        String location = "dummy";
        String modelId = "claude-opus-4-6";

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();

        // See: https://docs.cloud.google.com/vertex-ai/generative-ai/docs/partner-models/claude
        String vertexUrl = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s",
                location, projectId, location
        );

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(vertexUrl)
                .apiKey(token.getTokenValue())
                .modelName("anthropic/" + modelId)
                .logRequests(true)
                .logResponses(true)
                .build();

        System.out.println("--- Starting ---");
        String response = model.chat("Hi Claude ! Can you confirm that you run using Vertex AI through OpenAI ?");
        System.out.println("Response : " + response);
    }
}