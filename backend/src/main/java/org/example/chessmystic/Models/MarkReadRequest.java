package org.example.chessmystic.Models;

public class MarkReadRequest {
    private String messageId;
    private String userId;
    
    // Constructors
    public MarkReadRequest() {}
    
    public MarkReadRequest(String messageId, String userId) {
        this.messageId = messageId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
