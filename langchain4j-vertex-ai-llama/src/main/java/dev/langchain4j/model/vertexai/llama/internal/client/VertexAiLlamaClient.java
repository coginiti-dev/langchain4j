package dev.langchain4j.model.vertexai.llama.internal.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaRequest;
import dev.langchain4j.model.vertexai.llama.internal.api.LlamaResponse;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class VertexAiLlamaClient {

    private static final String VERTEX_AI_ENDPOINT_TEMPLATE = 
        "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/meta/models/%s:rawPredict";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private GoogleCredentials credentials;

    public VertexAiLlamaClient(String project, String location, String model) {
        this(project, location, model, null);
    }

    public VertexAiLlamaClient(String project, String location, String model, GoogleCredentials credentials) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.endpoint = String.format(VERTEX_AI_ENDPOINT_TEMPLATE, location, project, location, model);
        
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
