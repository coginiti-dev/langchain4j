package dev.langchain4j.model.vertexai.llama.internal.mapper;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.vertexai.llama.internal.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LlamaRequestMapper {

    public static LlamaRequest toLlamaRequest(
            String modelName,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications,
            Integer maxTokens,
            Double temperature,
            Double topP,
            Integer topK,
            List<String> stopSequences) {

        LlamaRequest request = new LlamaRequest();
        request.setModel(modelName);
        request.setMessages(toLlamaMessages(messages));
        request.setMaxTokens(maxTokens);
        request.setTemperature(temperature);
        request.setTopP(topP);
        request.setTopK(topK);
        request.setStop(stopSequences);
        
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            request.setTools(toLlamaTools(toolSpecifications));
        }

        return request;
    }

    private static List<LlamaMessage> toLlamaMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(LlamaRequestMapper::toLlamaMessage)
                .collect(Collectors.toList());
    }

    private static LlamaMessage toLlamaMessage(ChatMessage message) {
        String role = toLlamaRole(message);
        
        if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            if (userMessage.hasSingleText()) {
                return new LlamaMessage(role, userMessage.singleText());
            } else {
                return new LlamaMessage(role, toLlamaContents(userMessage.contents()));
            }
        } else if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) message;
            return new LlamaMessage(role, aiMessage.text());
        } else if (message instanceof SystemMessage) {
            SystemMessage systemMessage = (SystemMessage) message;
            return new LlamaMessage("system", systemMessage.text());
        }
        
        throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getSimpleName());
    }

    private static String toLlamaRole(ChatMessage message) {
        if (message instanceof UserMessage) {
            return "user";
        } else if (message instanceof AiMessage) {
            return "assistant";
        } else if (message instanceof SystemMessage) {
            return "system";
        }
        throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getSimpleName());
    }

    private static List<LlamaContent> toLlamaContents(List<Content> contents) {
        List<LlamaContent> llamaContents = new ArrayList<>();
        
        for (Content content : contents) {
            if (content instanceof TextContent) {
                TextContent textContent = (TextContent) content;
                llamaContents.add(new LlamaContent("text", textContent.text()));
            } else if (content instanceof ImageContent) {
                ImageContent imageContent = (ImageContent) content;
                String base64Data = imageContent.image().base64Data() != null ? 
                        imageContent.image().base64Data() : "";
                String imageUrl = "data:" + imageContent.image().mimeType() + ";base64," + base64Data;
                llamaContents.add(new LlamaContent("image_url", new LlamaImageUrl(imageUrl)));
            }
        }
        
        return llamaContents;
    }

    private static List<LlamaTool> toLlamaTools(List<ToolSpecification> toolSpecifications) {
        return toolSpecifications.stream()
                .map(toolSpec -> new LlamaTool("function", 
                    new LlamaTool.LlamaFunction(
                        toolSpec.name(), 
                        toolSpec.description(), 
                        toolSpec.parameters())))
                .collect(Collectors.toList());
    }
}
