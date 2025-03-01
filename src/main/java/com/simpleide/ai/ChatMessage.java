package com.simpleide.ai;

public class ChatMessage {
    public enum Role {
        USER, AI
    }
    
    private Role role;
    private String content;
    private long timestamp;
    
    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public Role getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
} 