package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaToolChoice {

    private String type;
    private LlamaToolChoiceFunction function;

    public LlamaToolChoice() {
    }

    public LlamaToolChoice(String type) {
        this.type = type;
    }

    public LlamaToolChoice(String type, LlamaToolChoiceFunction function) {
        this.type = type;
        this.function = function;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LlamaToolChoiceFunction getFunction() {
        return function;
    }

    public void setFunction(LlamaToolChoiceFunction function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return "LlamaToolChoice{" +
                "type='" + type + '\'' +
                ", function=" + function +
                '}';
    }

    public static class LlamaToolChoiceFunction {
        private String name;

        public LlamaToolChoiceFunction() {
        }

        public LlamaToolChoiceFunction(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "LlamaToolChoiceFunction{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
