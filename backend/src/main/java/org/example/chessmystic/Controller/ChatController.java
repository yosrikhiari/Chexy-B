package org.example.chessmystic.Controller;

import org.example.chessmystic.Config.websocket.RabbitMQMessageService;
import org.example.chessmystic.Models.*;
import org.example.chessmystic.Service.implementation.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {
    
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatService chatService;

    @Autowired
    private RabbitMQMessageService rabbitMQMessageService;



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
                "/queue/chat." + request.getSenderId(),
                savedMessage
            );

            // Send to receiver
            messagingTemplate.convertAndSend(
                "/queue/chat." + request.getReceiverId(),
                savedMessage
            );

            rabbitMQMessageService.sendChatMessage(request.getReceiverId(), savedMessage);

            log.info("Chat message sent successfully: {}", savedMessage.getId());
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage(), e);

            // Send error response to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message");
            errorResponse.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                    "/queue/chat." + request.getSenderId(),
                    errorResponse
            );
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
                Map<String, Object> readNotification = new HashMap<>();
                readNotification.put("type", "MESSAGE_READ");
                readNotification.put("messageId", message.getId());
                readNotification.put("readBy", request.getUserId());
                readNotification.put("timestamp", System.currentTimeMillis());

                messagingTemplate.convertAndSend(
                        "/queue/chat." + message.getSenderId(),
                        readNotification
                );
            }
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
        }
    }
}
