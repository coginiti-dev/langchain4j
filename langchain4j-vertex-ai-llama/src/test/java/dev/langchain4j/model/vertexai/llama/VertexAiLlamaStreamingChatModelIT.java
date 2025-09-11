package dev.langchain4j.model.vertexai.llama;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".+")
class VertexAiLlamaStreamingChatModelIT {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT_ID");
    private static final String LOCATION = "us-central1";
    private static final String MODEL_NAME = "meta/llama-3.1-8b-instruct-maas";

    @Test
    void should_stream_answer() throws Exception {
        StreamingChatModel model = VertexAiLlamaStreamingChatModel.builder()
                .project(PROJECT_ID)
                .location(LOCATION)
                .modelName(MODEL_NAME)
                .maxTokens(100)
                .temperature(0.7)
                .build();

        UserMessage userMessage = UserMessage.from("What is the capital of France?");
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .build();

        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        
        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        };

        model.chat(chatRequest, handler);

        ChatResponse response = future.get(30, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        assertThat(response.aiMessage().text()).containsIgnoringCase("Paris");
    }
}
