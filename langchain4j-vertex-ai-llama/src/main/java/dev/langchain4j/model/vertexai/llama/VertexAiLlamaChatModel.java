package dev.langchain4j.model.vertexai.llama;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.model.ModelProvider.GOOGLE_VERTEX_AI_LLAMA;

import com.google.auth.oauth2.GoogleCredentials;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ChatRequestValidationUtils;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaRequest;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaResponse;
import dev.langchain4j.model.vertexai.llama.internal.client.VertexAiLlamaClient;
import dev.langchain4j.model.vertexai.llama.internal.mapper.LlamaRequestMapper;
import dev.langchain4j.model.vertexai.llama.internal.mapper.LlamaResponseMapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Google Vertex AI Llama language model with a chat completion interface.
 * Supports Meta Llama models through Vertex AI's Model Garden.
 * <br>
 * Please follow these steps before using this model:
 * <br>
 * 1. <a href="https://github.com/googleapis/google-cloud-java?tab=readme-ov-file#local-developmenttesting">Authentication</a>
 * <br>
 * When developing locally, you can use one of:
 * <br>
 * a) <a href="https://github.com/googleapis/google-cloud-java?tab=readme-ov-file#local-developmenttesting">Google Cloud SDK</a>
 * <br>
 * b) <a href="https://github.com/googleapis/google-cloud-java?tab=readme-ov-file#using-a-service-account-recommended">Service account</a>
 * When using service account, ensure that <code>GOOGLE_APPLICATION_CREDENTIALS</code> environment variable points to your JSON service account key.
 * <br>
 * 2. <a href="https://cloud.google.com/vertex-ai/docs/model-garden/explore-models">Enable Vertex AI Model Garden</a>
 * <br>
 * 3. Request access to Meta Llama models in Vertex AI Model Garden
 */
public class VertexAiLlamaChatModel implements ChatModel, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(VertexAiLlamaChatModel.class);

    private final VertexAiLlamaClient client;
    private final String modelName;
    private final Integer maxTokens;
    private final Double temperature;
    private final Double topP;
    private final Integer topK;
    private final List<String> stopSequences;
    private final Boolean logRequests;
    private final Boolean logResponses;
    private final List<ChatModelListener> listeners;

    public VertexAiLlamaChatModel(VertexAiLlamaChatModelBuilder builder) {
        this.client = new VertexAiLlamaClient(
                ensureNotBlank(builder.project, "project"),
                ensureNotBlank(builder.location, "location"),
                ensureNotBlank(builder.modelName, "modelName"),
                builder.credentials
        );
        this.modelName = builder.modelName;
        this.maxTokens = getOrDefault(builder.maxTokens, 4096);
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.stopSequences = builder.stopSequences;
        this.logRequests = getOrDefault(builder.logRequests, false);
        this.logResponses = getOrDefault(builder.logResponses, false);
        this.listeners = builder.listeners != null ? List.copyOf(builder.listeners) : List.of();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        ChatRequestParameters parameters = chatRequest.parameters();
        ChatRequestValidationUtils.validateParameters(parameters);
        ChatRequestValidationUtils.validate(parameters.toolChoice());

        List<ChatMessage> messages = chatRequest.messages();
        List<ToolSpecification> toolSpecifications = parameters.toolSpecifications();

        ChatModelRequestContext requestContext = new ChatModelRequestContext(
                chatRequest,
                provider(),
                new ConcurrentHashMap<>());

        ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<>();
        requestContext.attributes().putAll(attributes);

        notifyListenersOnRequest(requestContext);

        try {
            LlamaRequest llamaRequest = LlamaRequestMapper.toLlamaRequest(
                    modelName,
                    messages,
                    toolSpecifications,
                    maxTokens,
                    temperature,
                    topP,
                    topK,
                    stopSequences
            );

            if (logRequests) {
                logger.debug("Llama request: {}", llamaRequest);
            }

            LlamaResponse llamaResponse = client.generateContent(llamaRequest);

            if (logResponses) {
                logger.debug("Llama response: {}", llamaResponse);
            }

            ChatResponse chatResponse = LlamaResponseMapper.toChatResponse(llamaResponse);

            ChatModelResponseContext responseContext = new ChatModelResponseContext(
                    chatResponse,
                    chatRequest,
                    provider(),
                    attributes);

            notifyListenersOnResponse(responseContext);

            return chatResponse;

        } catch (Exception e) {
            ChatModelErrorContext errorContext = new ChatModelErrorContext(
                    e,
                    chatRequest,
                    provider(),
                    attributes);

            notifyListenersOnError(errorContext);

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("Failed to generate response", e);
            }
        }
    }

    @Override
    public ModelProvider provider() {
        return GOOGLE_VERTEX_AI_LLAMA;
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private void notifyListenersOnRequest(ChatModelRequestContext requestContext) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                logger.warn("Exception in listener", e);
            }
        }
    }

    private void notifyListenersOnResponse(ChatModelResponseContext responseContext) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onResponse(responseContext);
            } catch (Exception e) {
                logger.warn("Exception in listener", e);
            }
        }
    }

    private void notifyListenersOnError(ChatModelErrorContext errorContext) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onError(errorContext);
            } catch (Exception e) {
                logger.warn("Exception in listener", e);
            }
        }
    }

    public static VertexAiLlamaChatModelBuilder builder() {
        return new VertexAiLlamaChatModelBuilder();
    }

    public static class VertexAiLlamaChatModelBuilder {
        private String project;
        private String location;
        private String modelName;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private Integer topK;
        private List<String> stopSequences;
        private Boolean logRequests;
        private Boolean logResponses;
        private List<ChatModelListener> listeners;
        private GoogleCredentials credentials;

        public VertexAiLlamaChatModelBuilder project(String project) {
            this.project = project;
            return this;
        }

        public VertexAiLlamaChatModelBuilder location(String location) {
            this.location = location;
            return this;
        }

        public VertexAiLlamaChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public VertexAiLlamaChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public VertexAiLlamaChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public VertexAiLlamaChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public VertexAiLlamaChatModelBuilder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public VertexAiLlamaChatModelBuilder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public VertexAiLlamaChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public VertexAiLlamaChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public VertexAiLlamaChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        /**
         * Sets the Google credentials to use for authentication.
         * If not provided, the client will use Application Default Credentials.
         *
         * @param credentials the Google credentials to use
         * @return this builder
         */
        public VertexAiLlamaChatModelBuilder credentials(GoogleCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public VertexAiLlamaChatModel build() {
            return new VertexAiLlamaChatModel(this);
        }
    }
}
