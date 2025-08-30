package org.example.chessmystic.Controller;

import org.example.chessmystic.Config.websocket.RabbitMQMessageService;
import org.example.chessmystic.Models.ChatMessage;
import org.example.chessmystic.Models.ChatMessageRequest;
import org.example.chessmystic.Models.ChatHistoryRequest;
import org.example.chessmystic.Models.MarkReadRequest;
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
        log.info("Fetching chat history between {} and {}", request.getUserId1(), request.getUserId2());

        try {
            List<ChatMessage> history = chatService.getChatHistory(
                    request.getUserId1(),
                    request.getUserId2()
            );

            log.info("Found {} messages in chat history between {} and {}",
                    history.size(), request.getUserId1(), request.getUserId2());

            // Create response object with metadata
            Map<String, Object> historyResponse = new HashMap<>();
            historyResponse.put("messages", history);
            historyResponse.put("friendId", request.getUserId2());
            historyResponse.put("userId", request.getUserId1());
            historyResponse.put("timestamp", System.currentTimeMillis());

            // Send history to requesting user
            messagingTemplate.convertAndSend(
                    "/queue/chat.history." + request.getUserId1(),
                    historyResponse
            );

            log.info("Chat history sent to user {}", request.getUserId1());

        } catch (Exception e) {
            log.error("Error fetching chat history: {}", e.getMessage(), e);

            // Send error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load chat history");
            errorResponse.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                    "/queue/chat.history." + request.getUserId1(),
                    errorResponse
            );
        }
    }
}
