package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.ChatMessage;
import org.example.chessmystic.Models.ChatMessageRequest;
import org.example.chessmystic.Models.ChatHistoryRequest;
import org.example.chessmystic.Models.MarkReadRequest;
import org.example.chessmystic.Service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatController {
    
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatService chatService;
    
    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageRequest request) {
        log.info("Chat message received from {} to {}", request.getSenderId(), request.getReceiverId());
        
        try {
            // Save message to database
            ChatMessage savedMessage = chatService.saveMessage(
                request.getSenderId(),
                request.getSenderName(),
                request.getReceiverId(),
                request.getMessage()
            );
            
            // Send to sender (for confirmation)
            messagingTemplate.convertAndSend(
                "/queue/chat/" + request.getSenderId(),
                savedMessage
            );
            
            // Send to receiver
            messagingTemplate.convertAndSend(
                "/queue/chat/" + request.getReceiverId(),
                savedMessage
            );
            
            log.info("Chat message sent successfully: {}", savedMessage.getId());
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage(), e);
        }
    }
    
    @MessageMapping("/chat/mark-read")
    public void markAsRead(@Payload MarkReadRequest request) {
        log.info("Marking message {} as read by user {}", request.getMessageId(), request.getUserId());
        
        try {
            chatService.markMessageAsRead(request.getMessageId(), request.getUserId());
            
            // Notify the sender that their message was read
            ChatMessage message = chatService.getMessageById(request.getMessageId());
            if (message != null) {
                messagingTemplate.convertAndSend(
                    "/queue/chat/read/" + message.getSenderId(),
                    message
                );
            }
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
        }
    }
    
    @MessageMapping("/chat/history")
    public void getChatHistory(@Payload ChatHistoryRequest request) {
        log.info("Fetching chat history between {} and {}", request.getUserId1(), request.getUserId2());
        
        try {
            List<ChatMessage> history = chatService.getChatHistory(
                request.getUserId1(),
                request.getUserId2()
            );
            
            // Send history to both users
            messagingTemplate.convertAndSend(
                "/queue/chat/history/" + request.getUserId1(),
                history
            );
            
            messagingTemplate.convertAndSend(
                "/queue/chat/history/" + request.getUserId2(),
                history
            );
        } catch (Exception e) {
            log.error("Error fetching chat history: {}", e.getMessage(), e);
        }
    }
}
