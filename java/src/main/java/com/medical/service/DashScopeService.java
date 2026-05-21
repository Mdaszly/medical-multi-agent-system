package com.medical.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

//1. DashScope服务，封装阿里云通义千问API调用
//2. 支持标准ChatML格式的消息结构
//3. 处理多种响应格式（DashScope原生格式和OpenAI兼容格式）
@Slf4j
@Service
public class DashScopeService {

    //4. API密钥（从配置文件读取）
    private final String apiKey;
    
    //5. 模型名称（默认qwen-max）
    private final String model;

    private final String compatibleBaseUrl;

    private final long timeoutMs;
    
    //6. JSON解析器
    private final ObjectMapper objectMapper;
    
    //7. HTTP客户端
    private final HttpClient httpClient;

    //8. 构造函数：注入配置参数
    public DashScopeService(
            @Value("${app.dashscope.api-key:}") String apiKey,
            @Value("${app.dashscope.model:qwen-max}") String model,
            @Value("${app.dashscope.compatible-base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
            String compatibleBaseUrl,
            @Value("${app.dashscope.timeout-ms:60000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.compatibleBaseUrl = compatibleBaseUrl;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    //9. 核心方法：调用DashScope API生成响应
    public String generate(String systemPrompt, String userPrompt) throws Exception {
        //10. 构建system消息
        Map<String, Object> message1 = new HashMap<>();
        message1.put("role", "system");
        message1.put("content", systemPrompt);

        //11. 构建user消息
        Map<String, Object> message2 = new HashMap<>();
        message2.put("role", "user");
        message2.put("content", userPrompt);

        //12. 构建input对象
        Map<String, Object> input = new HashMap<>();
        input.put("messages", List.of(message1, message2));

        //13. 构建参数（temperature=0.2保证输出确定性）
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.2);
        parameters.put("max_tokens", 2048);

        //14. 构建完整请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);

        //15. 序列化为JSON字符串
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.info("DashScope request body: {}", jsonBody);

        //16. 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        //17. 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("DashScope response status: {}", response.statusCode());
        log.info("DashScope response body: {}", response.body());

        //18. 检查HTTP状态码
        if (response.statusCode() != 200) {
            throw new RuntimeException("DashScope API error (status " + response.statusCode() + "): " + response.body());
        }

        //19. 解析响应JSON
        JsonNode root = objectMapper.readTree(response.body());
        
        //20. 提取output字段
        JsonNode output = root.get("output");
        if (output == null) {
            throw new RuntimeException("DashScope response missing 'output' field: " + response.body());
        }
        
        //21. 提取content（支持两种响应格式）
        String content = null;
        if (output.has("text")) {
            // DashScope原生格式
            content = output.get("text").asText();
        } else if (output.has("choices")) {
            // OpenAI兼容格式
            JsonNode choices = output.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    content = firstChoice.get("message").get("content").asText();
                } else if (firstChoice.has("text")) {
                    content = firstChoice.get("text").asText();
                }
            }
        }
        
        //22. 检查content是否成功提取
        if (content == null) {
            throw new RuntimeException("DashScope response missing content: " + response.body());
        }
        
        return content;
    }

    /**
     * 百炼 OpenAI 兼容模式流式输出（SSE）
     */
    public void generateStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) throws Exception {
        Map<String, Object> message1 = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> message2 = Map.of("role", "user", "content", userPrompt);
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "stream", true,
                "temperature", 0.2,
                "max_tokens", 2048,
                "messages", List.of(message1, message2)
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(compatibleBaseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("DashScope stream error (status " + response.statusCode() + "): " + errorBody);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode contentNode = choices.get(0).path("delta").path("content");
                if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                    String delta = contentNode.asText();
                    if (!delta.isEmpty()) {
                        onChunk.accept(delta);
                    }
                }
            }
        }
    }

    //23. 检查API密钥是否已配置
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 百炼 OpenAI 兼容模式文本向量（用于症状语义检索）
     */
    public float[] embed(String text, String embeddingModel) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("DashScope API key not configured");
        }
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", text.trim()
        );
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(compatibleBaseUrl + "/embeddings"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("DashScope embedding error (status " + response.statusCode() + "): "
                    + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new RuntimeException("DashScope embedding response missing data: " + response.body());
        }
        JsonNode embeddingNode = data.get(0).path("embedding");
        if (!embeddingNode.isArray()) {
            throw new RuntimeException("DashScope embedding response missing embedding array: " + response.body());
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }

    public List<float[]> embedBatch(List<String> texts, String embeddingModel) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("DashScope API key not configured");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", texts.stream().map(String::trim).toList()
        );
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(compatibleBaseUrl + "/embeddings"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("DashScope embedding batch error (status " + response.statusCode() + "): "
                    + response.body());
        }
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        if (!data.isArray()) {
            throw new RuntimeException("DashScope embedding batch response missing data: " + response.body());
        }
        List<float[]> vectors = new java.util.ArrayList<>();
        for (JsonNode item : data) {
            JsonNode embeddingNode = item.path("embedding");
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            vectors.add(vector);
        }
        return vectors;
    }
}