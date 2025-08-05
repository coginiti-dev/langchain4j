package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaMessage {

    private String role;
    private String content;
    private List<LlamaContent> contents;

    public LlamaMessage() {
    }

    public LlamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public LlamaMessage(String role, List<LlamaContent> contents) {
        this.role = role;
        this.contents = contents;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<LlamaContent> getContents() {
        return contents;
    }

    public void setContents(List<LlamaContent> contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "LlamaMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", contents=" + contents +
                '}';
    }
}
