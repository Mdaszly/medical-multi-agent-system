package com.medical.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    //6. JSON解析器
    private final ObjectMapper objectMapper;
    
    //7. HTTP客户端
    private final HttpClient httpClient;

    //8. 构造函数：注入配置参数
    public DashScopeService(
            @Value("${app.dashscope.api-key:}") String apiKey,
            @Value("${app.dashscope.model:qwen-max}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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

    //23. 检查API密钥是否已配置
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}