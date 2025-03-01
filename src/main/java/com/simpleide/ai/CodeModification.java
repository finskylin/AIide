package com.simpleide.ai;

public class CodeModification {
    private String filePath;
    private String methodName;
    private int startLine;
    private int endLine;
    private String originalCode;
    private String modifiedCode;
    private String description;
    
    public CodeModification(String filePath, String methodName, 
                          String originalCode, String modifiedCode, String description) {
        this.filePath = filePath;
        this.methodName = methodName;
        this.originalCode = originalCode;
        this.modifiedCode = modifiedCode;
        this.description = description;
    }
    
    public CodeModification(String filePath, String modifiedCode) {
        this.filePath = filePath;
        this.modifiedCode = modifiedCode;
    }
    
    // Getters
    public String getFilePath() { return filePath; }
    public String getMethodName() { return methodName; }
    public String getOriginalCode() { return originalCode; }
    public String getModifiedCode() { return modifiedCode; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return filePath;
    }
} 