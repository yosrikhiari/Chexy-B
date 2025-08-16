package org.example.chessmystic.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {
    
    @Id
    private String id;
    
    @Indexed
    private String senderId;
    
    private String senderName;
    
    @Indexed
    private String receiverId;
    
    private String message;
    
    private LocalDateTime timestamp;
    
    private boolean isRead;
    
    // Constructors
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }
    
    public ChatMessage(String senderId, String senderName, String receiverId, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
}
