package org.example.chessmystic.Models;

public class ChatMessageRequest {
    private String senderId;
    private String senderName;
    private String receiverId;
    private String message;
    
    // Constructors
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(String senderId, String senderName, String receiverId, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.message = message;
    }
    
    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
