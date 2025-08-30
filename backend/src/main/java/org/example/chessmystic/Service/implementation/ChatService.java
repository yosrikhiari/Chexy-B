package org.example.chessmystic.Service.implementation;

import org.example.chessmystic.Models.ChatMessage;
import org.example.chessmystic.Repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    public ChatMessage saveMessage(String senderId, String senderName, String receiverId, String message) {
        ChatMessage chatMessage = new ChatMessage(senderId, senderName, receiverId, message);
        return chatMessageRepository.save(chatMessage);
    }
    
    public void markMessageAsRead(String messageId, String userId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            ChatMessage message = messageOpt.get();
            // Only mark as read if the receiver is marking it
            if (message.getReceiverId().equals(userId)) {
                message.setRead(true);
                chatMessageRepository.save(message);
            }
        }
    }
    
    public ChatMessage getMessageById(String messageId) {
        return chatMessageRepository.findById(messageId).orElse(null);
    }
    
    public List<ChatMessage> getChatHistory(String userId1, String userId2) {
        // Get messages between these two users (in both directions)
        List<ChatMessage> messages = chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestampAsc(
            userId1, userId2, userId1, userId2
        );
        
        // Mark messages as read for the requesting user
        messages.stream()
            .filter(msg -> msg.getReceiverId().equals(userId1) && !msg.isRead())
            .forEach(msg -> {
                msg.setRead(true);
                chatMessageRepository.save(msg);
            });
        
        return messages;
    }
    
    public List<ChatMessage> getUnreadMessages(String userId) {
        return chatMessageRepository.findByReceiverIdAndIsReadFalseOrderByTimestampDesc(userId);
    }
}
