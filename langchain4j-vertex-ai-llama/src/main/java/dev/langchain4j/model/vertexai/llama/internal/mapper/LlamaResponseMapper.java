package dev.langchain4j.model.vertexai.llama.internal.mapper;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaResponse;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaUsage;

public class LlamaResponseMapper {

    public static ChatResponse toChatResponse(LlamaResponse llamaResponse) {
        if (llamaResponse == null || llamaResponse.getChoices() == null || llamaResponse.getChoices().isEmpty()) {
            throw new IllegalArgumentException("Invalid Llama response");
        }

        LlamaResponse.LlamaChoice choice = llamaResponse.getChoices().get(0);
        
        AiMessage aiMessage = AiMessage.from(choice.getMessage().getContent());
        
        FinishReason finishReason = toFinishReason(choice.getFinishReason());
        
        TokenUsage tokenUsage = toTokenUsage(llamaResponse.getUsage());

        return ChatResponse.builder()
                .aiMessage(aiMessage)
                .finishReason(finishReason)
                .tokenUsage(tokenUsage)
                .build();
    }

    private static FinishReason toFinishReason(String llamaFinishReason) {
        if (llamaFinishReason == null) {
            return null;
        }
        
        switch (llamaFinishReason) {
            case "stop":
                return FinishReason.STOP;
            case "length":
                return FinishReason.LENGTH;
            case "tool_calls":
                return FinishReason.TOOL_EXECUTION;
            case "content_filter":
                return FinishReason.CONTENT_FILTER;
            default:
                return FinishReason.OTHER;
        }
    }

    private static TokenUsage toTokenUsage(LlamaUsage llamaUsage) {
        if (llamaUsage == null) {
            return null;
        }
        
        return new TokenUsage(
                llamaUsage.getPromptTokens(),
                llamaUsage.getCompletionTokens(),
                llamaUsage.getTotalTokens()
        );
    }
}
