package com.simpleide.ai;

public class AIServiceFactory {
    public enum ServiceType {
        DEEPSEEK,
        ALIYUN_DEEPSEEK
    }
    
    public static AIService createService(ServiceType type) {
        switch (type) {
            case DEEPSEEK:
                return new DeepseekService();
            case ALIYUN_DEEPSEEK:
                return new AliyunDeepseekService();
            default:
                throw new IllegalArgumentException("Unknown service type: " + type);
        }
    }
} 