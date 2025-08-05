package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<LlamaChoice> choices;
    private LlamaUsage usage;

    public LlamaResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<LlamaChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<LlamaChoice> choices) {
        this.choices = choices;
    }

    public LlamaUsage getUsage() {
        return usage;
    }

    public void setUsage(LlamaUsage usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        return "LlamaResponse{" +
                "id='" + id + '\'' +
                ", object='" + object + '\'' +
                ", created=" + created +
                ", model='" + model + '\'' +
                ", choices=" + choices +
                ", usage=" + usage +
                '}';
    }

    public static class LlamaChoice {
        private Integer index;
        private LlamaMessage message;
        private String finishReason;

        public LlamaChoice() {
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public LlamaMessage getMessage() {
            return message;
        }

        public void setMessage(LlamaMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public String toString() {
            return "LlamaChoice{" +
                    "index=" + index +
                    ", message=" + message +
                    ", finishReason='" + finishReason + '\'' +
                    '}';
        }
    }
}
