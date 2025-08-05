package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaRequest {

    private String model;
    private List<LlamaMessage> messages;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private List<String> stop;
    private Boolean stream;
    private List<LlamaTool> tools;
    private LlamaToolChoice toolChoice;

    public LlamaRequest() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<LlamaMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<LlamaMessage> messages) {
        this.messages = messages;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<LlamaTool> getTools() {
        return tools;
    }

    public void setTools(List<LlamaTool> tools) {
        this.tools = tools;
    }

    public LlamaToolChoice getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(LlamaToolChoice toolChoice) {
        this.toolChoice = toolChoice;
    }

    @Override
    public String toString() {
        return "LlamaRequest{" +
                "model='" + model + '\'' +
                ", messages=" + messages +
                ", maxTokens=" + maxTokens +
                ", temperature=" + temperature +
                ", topP=" + topP +
                ", topK=" + topK +
                ", stop=" + stop +
                ", stream=" + stream +
                ", tools=" + tools +
                ", toolChoice=" + toolChoice +
                '}';
    }
}
