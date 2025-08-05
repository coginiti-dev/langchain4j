package dev.langchain4j.model.vertexai.llama.internal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LlamaTool {

    private String type;
    private LlamaFunction function;

    public LlamaTool() {
    }

    public LlamaTool(String type, LlamaFunction function) {
        this.type = type;
        this.function = function;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LlamaFunction getFunction() {
        return function;
    }

    public void setFunction(LlamaFunction function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return "LlamaTool{" +
                "type='" + type + '\'' +
                ", function=" + function +
                '}';
    }

    public static class LlamaFunction {
        private String name;
        private String description;
        private Object parameters;

        public LlamaFunction() {
        }

        public LlamaFunction(String name, String description, Object parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getParameters() {
            return parameters;
        }

        public void setParameters(Object parameters) {
            this.parameters = parameters;
        }

        @Override
        public String toString() {
            return "LlamaFunction{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", parameters=" + parameters +
                    '}';
        }
    }
}
