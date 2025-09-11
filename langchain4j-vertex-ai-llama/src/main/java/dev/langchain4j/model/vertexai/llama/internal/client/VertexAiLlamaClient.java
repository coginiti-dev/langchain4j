package dev.langchain4j.model.vertexai.llama.internal.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaRequest;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaResponse;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaMessage;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VertexAiLlamaClient {

    private static final String VERTEX_AI_ENDPOINT_TEMPLATE = 
        "https://%s-aiplatform.googleapis.com/v1beta1/projects/%s/locations/%s/endpoints/openapi/chat/completions";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private GoogleCredentials credentials;

    public VertexAiLlamaClient(String project, String location, GoogleCredentials credentials) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.endpoint = String.format(
                VERTEX_AI_ENDPOINT_TEMPLATE,
                location,
                project,
                location
        );
        
        if (credentials != null) {
            try {
                this.credentials = credentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
            } catch (Exception e) {
                throw new RuntimeException("Failed to scope provided Google credentials", e);
            }
        } else {
            try {
                this.credentials = GoogleCredentials.getApplicationDefault()
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize Google credentials", e);
            }
        }
    }

    public LlamaResponse generateContent(LlamaRequest request) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);
        
        String accessToken = getAccessToken();
        
        Request httpRequest = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + 
                                    " - " + response.body().string());
            }
            
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, LlamaResponse.class);
        }
    }

    public void generateContentStream(LlamaRequest request, 
                                     Consumer<LlamaResponse> onPartial,
                                     Consumer<LlamaResponse> onComplete,
                                     Consumer<Exception> onError) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);
        String accessToken = getAccessToken();
        
        Request httpRequest = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + 
                                    " - " + response.body().string());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            StringBuilder accumulatedContent = new StringBuilder();
            LlamaResponse finalResponse = null;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.source().inputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String jsonData = line.substring(6).trim();
                        
                        if ("[DONE]".equals(jsonData)) {
                            if (finalResponse != null) {
                                onComplete.accept(finalResponse);
                            }
                            break;
                        }
                        
                        if (!jsonData.isEmpty()) {
                            try {
                                LlamaResponse streamResponse = objectMapper.readValue(jsonData, LlamaResponse.class);
                                
                                if (streamResponse.getChoices() != null && !streamResponse.getChoices().isEmpty()) {
                                    LlamaResponse.LlamaChoice choice = streamResponse.getChoices().get(0);
                                    
                                    // For streaming, we need to check the delta field, not the message field
                                    if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                        String content = choice.getDelta().getContent();
                                        accumulatedContent.append(content);
                                        onPartial.accept(streamResponse);
                                    }
                                    
                                    if ("stop".equals(choice.getFinishReason())) {
                                        LlamaResponse completeResponse = createCompleteResponse(streamResponse, accumulatedContent.toString());
                                        finalResponse = completeResponse;
                                    }
                                }
                            } catch (Exception e) {
                                onError.accept(e);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        }
    }
    
    private LlamaResponse createCompleteResponse(LlamaResponse lastResponse, String fullContent) {
        LlamaResponse completeResponse = new LlamaResponse();
        completeResponse.setId(lastResponse.getId());
        completeResponse.setObject(lastResponse.getObject());
        completeResponse.setCreated(lastResponse.getCreated());
        completeResponse.setModel(lastResponse.getModel());
        completeResponse.setUsage(lastResponse.getUsage());
        
        if (lastResponse.getChoices() != null && !lastResponse.getChoices().isEmpty()) {
            LlamaResponse.LlamaChoice originalChoice = lastResponse.getChoices().get(0);
            LlamaResponse.LlamaChoice completeChoice = new LlamaResponse.LlamaChoice();
            completeChoice.setIndex(originalChoice.getIndex());
            completeChoice.setFinishReason(originalChoice.getFinishReason());
            
            LlamaMessage completeMessage = new LlamaMessage();
            completeMessage.setRole("assistant");
            completeMessage.setContent(fullContent);
            completeChoice.setMessage(completeMessage);
            
            completeResponse.setChoices(List.of(completeChoice));
        }
        
        return completeResponse;
    }

    private String getAccessToken() throws IOException {
        credentials.refreshIfExpired();
        AccessToken accessToken = credentials.getAccessToken();
        if (accessToken == null) {
            throw new IOException("Failed to obtain access token");
        }
        return accessToken.getTokenValue();
    }

    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
