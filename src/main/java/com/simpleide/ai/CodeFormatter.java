package com.simpleide.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class CodeFormatter {
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:([^:\\n]+):)?([^\\n]+)?\\n([\\s\\S]*?)```");
    private static final Pattern MARKDOWN_HEADER_PATTERN = Pattern.compile("^###\\s+(.*)$", Pattern.MULTILINE);
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("^(?:src/main/(?:java|resources)/)?(.+)$");
    
    private static final Map<String, String> FILE_TYPE_MAP = new HashMap<>();
    static {
        // Java files
        FILE_TYPE_MAP.put("java", "text/java");
        
        // Build and config files
        FILE_TYPE_MAP.put("xml", "text/xml");
        FILE_TYPE_MAP.put("pom.xml", "text/xml");
        FILE_TYPE_MAP.put("properties", "text/properties");
        FILE_TYPE_MAP.put("yml", "text/yaml");
        FILE_TYPE_MAP.put("yaml", "text/yaml");
        FILE_TYPE_MAP.put("json", "text/json");
        
        // Web files
        FILE_TYPE_MAP.put("html", "text/html");
        FILE_TYPE_MAP.put("css", "text/css");
        FILE_TYPE_MAP.put("js", "text/javascript");
        
        // Database files
        FILE_TYPE_MAP.put("sql", "text/sql");
        
        // Documentation files
        FILE_TYPE_MAP.put("md", "text/markdown");
        
        // Shell scripts
        FILE_TYPE_MAP.put("sh", "text/unix");
    }
    
    public static String formatResponse(String response) {
        // 首先处理文件路径标记
        Pattern filePathPattern = Pattern.compile("===\\s+([^=\\n]+)\\s+===");
        Matcher filePathMatcher = filePathPattern.matcher(response);
        Map<String, String> filePathMap = new HashMap<>();
        
        while (filePathMatcher.find()) {
            String filePath = filePathMatcher.group(1).trim();
            // 存储文件路径以供后续使用
            filePathMap.put(getFileNameFromPath(filePath), filePath);
        }
        
        // 移除Markdown标题标记，保留标题文本
        response = MARKDOWN_HEADER_PATTERN.matcher(response).replaceAll("$1");
        
        // 处理代码块
        StringBuffer sb = new StringBuffer();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(response);
        
        while (matcher.find()) {
            String language = matcher.group(1);
            String filePath = matcher.group(2);
            String code = matcher.group(3);
            
            // 如果在文件路径映射中找到对应的完整路径，使用它
            if (filePath != null) {
                String fileName = getFileNameFromPath(filePath);
                if (filePathMap.containsKey(fileName)) {
                    filePath = filePathMap.get(fileName);
                }
            }
            
            // 如果没有指定语言，从文件路径推断
            if (language == null && filePath != null) {
                language = getLanguageFromFilePath(filePath);
            }
            
            // 创建格式化后的代码块
            StringBuilder formattedCode = new StringBuilder();
            if (filePath != null) {
                formattedCode.append("```").append(language).append(":").append(filePath).append("\n");
            } else {
                formattedCode.append("```").append(language != null ? language : "").append("\n");
            }
            formattedCode.append(code.trim()).append("\n```");
            
            // 替换原始的代码块
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formattedCode.toString()));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    public static String extractFilePath(String codeBlock) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(codeBlock);
        if (matcher.find() && matcher.group(2) != null) {
            String filePath = matcher.group(2);
            // 处理文件路径
            Matcher pathMatcher = FILE_PATH_PATTERN.matcher(filePath);
            return pathMatcher.matches() ? pathMatcher.group(1) : filePath;
        }
        return null;
    }
    
    public static String extractCode(String codeBlock) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(3).trim();
        }
        return codeBlock;
    }
    
    private static String getLanguageFromFilePath(String filePath) {
        if (filePath == null) return "";
        
        // 特殊处理 pom.xml
        if (filePath.endsWith("pom.xml")) {
            return FILE_TYPE_MAP.get("pom.xml");
        }
        
        // 从文件扩展名获取语言类型
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > -1 && lastDotIndex < filePath.length() - 1) {
            String extension = filePath.substring(lastDotIndex + 1).toLowerCase();
            return FILE_TYPE_MAP.getOrDefault(extension, extension);
        }
        
        return "";
    }
    
    public static String getFileType(String filePath) {
        return getLanguageFromFilePath(filePath);
    }
    
    private static String getFileNameFromPath(String path) {
        if (path == null) return "";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
} 