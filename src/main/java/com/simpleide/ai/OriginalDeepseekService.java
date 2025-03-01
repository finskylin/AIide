package com.simpleide.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OriginalDeepseekService implements AIService {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 120;
    
    public OriginalDeepseekService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    @Override
    public String getAIResponse(String prompt) throws IOException {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                return sendRequest(prompt);
            } catch (IOException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    throw e;
                }
                System.out.println("请求失败，正在进行第 " + retries + " 次重试...");
                try {
                    Thread.sleep(1000 * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试被中断", e);
                }
            }
        }
        throw new IOException("达到最大重试次数");
    }
    
    private String sendRequest(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-reasoner");
        requestBody.put("stream", false);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", 
            "你是一个专业的Java编程助手。请遵循以下规则回复：\n" +
            "1. 使用Markdown格式组织回复内容\n" +
            "2. 代码修改建议要使用=== 完整文件名 ===格式清晰指出针对哪个文件\n" +
            "3. 代码块使用```代码类型:完整文件名 格式\n" +
            "4. 确保代码格式规范，使用正确的缩进\n" +
            "5. 如果需要修改多个文件，请分别用不同的代码块表示\n" +
            "6. 在每个代码块前说明修改的原因和目的\n" +
            "7. 不需要说明使用案例，只需要说明实现过程和具体代码"
        );
        messages.add(systemMessage);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        String jsonRequest = gson.toJson(requestBody);
        System.out.println("发送请求内容:\n" + jsonRequest);
        
        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                jsonRequest
            ))
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            System.out.println("收到API响应状态码: " + response.code());
            
            if (!response.isSuccessful()) {
                ResponseBody errorBody = response.body();
                String errorContent = errorBody != null ? errorBody.string() : "No error body";
                System.err.println("错误响应内容:\n" + errorContent);
                throw new IOException("Unexpected response " + response + "\nError body: " + errorContent);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            String responseContent = responseBody.string();
            System.out.println("API响应内容:\n" + responseContent);
            
            try {
                Map<String, Object> responseMap = gson.fromJson(responseContent, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new IOException("No choices in response");
                }
                
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, String> assistantMessage = (Map<String, String>) firstChoice.get("message");
                if (assistantMessage == null) {
                    throw new IOException("No message in first choice");
                }
                
                String content = assistantMessage.get("content");
                if (content == null) {
                    throw new IOException("No content in message");
                }
                
                // 使用CodeFormatter处理响应内容
                return CodeFormatter.formatResponse(content);
            } catch (Exception e) {
                System.err.println("解析响应失败:\n" + responseContent);
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void testConnection() {
        try {
            String response = getAIResponse("Hello, can you help me with Java programming?");
            System.out.println("AI Response:\n" + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 