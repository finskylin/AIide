package com.simpleide.formatter;

import java.util.regex.Pattern;

public class JavaCodeFormatter {
    private static final Pattern INDENT_PATTERN = Pattern.compile("\\{[^}]*$");
    private static final Pattern DEDENT_PATTERN = Pattern.compile("^[^{]*}");
    
    public String format(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return sourceCode;
        }

        StringBuilder formatted = new StringBuilder();
        String[] lines = sourceCode.split("\\n");
        int indentLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                formatted.append("\n");
                continue;
            }
            
            // Check if we need to decrease indent
            if (DEDENT_PATTERN.matcher(trimmed).find()) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            // Add indentation
            for (int i = 0; i < indentLevel; i++) {
                formatted.append("    ");
            }
            formatted.append(trimmed).append("\n");
            
            // Check if we need to increase indent
            if (INDENT_PATTERN.matcher(trimmed).find()) {
                indentLevel++;
            }
        }
        
        return formatted.toString();
    }

    public static String formatResponse(String response) {
        // 这里可以添加响应格式化逻辑
        return response;
    }
} 