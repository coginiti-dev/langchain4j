package dev.langchain4j.model.vertexai.llama.spi;

import dev.langchain4j.model.vertexai.llama.VertexAiLlamaStreamingChatModel;

public class VertexAiLlamaStreamingChatModelBuilderFactory {

    public VertexAiLlamaStreamingChatModel.VertexAiLlamaStreamingChatModelBuilder get() {
        return VertexAiLlamaStreamingChatModel.builder();
    }
}
