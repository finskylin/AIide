package com.simpleide.ai;

import java.io.IOException;

public interface AIService {
    String getAIResponse(String prompt) throws IOException;
    void testConnection();
} 