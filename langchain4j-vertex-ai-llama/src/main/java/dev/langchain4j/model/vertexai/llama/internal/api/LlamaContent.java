package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaContent {

    private String type;
    private String text;
    private LlamaImageUrl imageUrl;

    public LlamaContent() {
    }

    public LlamaContent(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public LlamaContent(String type, LlamaImageUrl imageUrl) {
        this.type = type;
        this.imageUrl = imageUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LlamaImageUrl getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(LlamaImageUrl imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "LlamaContent{" +
                "type='" + type + '\'' +
                ", text='" + text + '\'' +
                ", imageUrl=" + imageUrl +
                '}';
    }
}
