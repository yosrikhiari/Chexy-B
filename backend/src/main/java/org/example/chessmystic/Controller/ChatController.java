package org.example.chessmystic.Controller;

import org.example.chessmystic.Config.RabbitMQ.RabbitMQMessageService;
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

    @MessageMapping("/chat/history")
    public void getChatHistory(@Payload ChatHistoryRequest request) {
        try {
            log.info("Fetching chat history between {} and {}", request.getUserId1(), request.getUserId2());

            List<ChatMessage> messages = chatService.getChatHistory(request.getUserId1(), request.getUserId2());

            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_HISTORY");
            response.put("friendId", request.getUserId2());
            response.put("messages", messages);

            // Send chat history back to the requester
            messagingTemplate.convertAndSend(
                    "/queue/chat.history." + request.getUserId1(),
                    response
            );
        } catch (Exception e) {
            log.error("Error fetching chat history: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch chat history");
            errorResponse.put("timestamp", System.currentTimeMillis());

            // Best-effort send to requester if available
            if (request != null && request.getUserId1() != null) {
                messagingTemplate.convertAndSend(
                        "/queue/chat.history." + request.getUserId1(),
                        errorResponse
                );
            }
        }
    }
}
