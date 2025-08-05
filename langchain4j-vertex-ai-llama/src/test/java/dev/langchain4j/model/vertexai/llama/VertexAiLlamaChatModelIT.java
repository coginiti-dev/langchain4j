package dev.langchain4j.model.vertexai.llama;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".+")
class VertexAiLlamaChatModelIT {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT_ID");
    private static final String LOCATION = "us-central1";
    private static final String MODEL_NAME = "llama-3.1-8b-instruct-maas";

    @Test
    void should_generate_answer() {
        ChatModel model = VertexAiLlamaChatModel.builder()
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

        ChatResponse response = model.chat(chatRequest);

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        assertThat(response.aiMessage().text()).containsIgnoringCase("Paris");
    }

    @Test
    void should_respect_max_tokens() {
        ChatModel model = VertexAiLlamaChatModel.builder()
                .project(PROJECT_ID)
                .location(LOCATION)
                .modelName(MODEL_NAME)
                .maxTokens(10)
                .build();

        UserMessage userMessage = UserMessage.from("Tell me a long story about adventures.");
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .build();

        ChatResponse response = model.chat(chatRequest);

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().outputTokenCount()).isLessThanOrEqualTo(10);
    }

    @Test
    void should_support_system_message() {
        ChatModel model = VertexAiLlamaChatModel.builder()
                .project(PROJECT_ID)
                .location(LOCATION)
                .modelName(MODEL_NAME)
                .maxTokens(50)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(
                        dev.langchain4j.data.message.SystemMessage.from("You are a helpful assistant that always responds in French."),
                        UserMessage.from("What is the capital of Spain?")
                )
                .build();

        ChatResponse response = model.chat(chatRequest);

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        String responseText = response.aiMessage().text().toLowerCase();
        assertThat(responseText).contains("madrid");
    }
}
