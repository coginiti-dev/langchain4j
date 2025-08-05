package dev.langchain4j.model.vertexai.llama.spi;

import dev.langchain4j.model.vertexai.llama.VertexAiLlamaChatModel;

public class VertexAiLlamaChatModelBuilderFactory {

    public VertexAiLlamaChatModel.VertexAiLlamaChatModelBuilder get() {
        return VertexAiLlamaChatModel.builder();
    }
}
