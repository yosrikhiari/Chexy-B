package org.example.chessmystic.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chessmystic.Config.RabbitMQ.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChessMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ChessMessageListener.class);

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.GAME_UPDATE_QUEUE, containerFactory = "messageListenerContainer")
    public void handleGameUpdate(Object gameUpdate) {
        try {
            logger.info("Received Game Update: {}", gameUpdate);
            messagingTemplate.convertAndSend("/topic/game-Updates", gameUpdate);
        } catch (Exception e) {
            logger.error("Error handling game update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE, containerFactory = "messageListenerContainer")
    public void handleChat(Object chatMessage) {
        try {
            logger.info("Received Chat Message: {}", chatMessage);

            // Parse the chat message to extract receiver ID
            String receiverId = null;
            try {
                if (chatMessage instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageMap = (Map<String, Object>) chatMessage;
                    receiverId = (String) messageMap.get("receiverId");
                } else {
                    // Convert to string and parse as JSON
                    String jsonString = objectMapper.writeValueAsString(chatMessage);
                    Map<String, Object> messageMap = objectMapper.readValue(jsonString, Map.class);
                    receiverId = (String) messageMap.get("receiverId");
                }
            } catch (Exception e) {
                logger.warn("Could not parse receiverId from chat message: {}", e.getMessage());
            }

            if (receiverId != null) {
                // Send to specific user's queue - this matches what the frontend expects
                String destination = "/queue/chat." + receiverId;
                logger.info("Sending chat message to destination: {}", destination);
                messagingTemplate.convertAndSend(destination, chatMessage);
            } else {
                // Fallback to general topic if we can't determine receiver
                logger.warn("No receiverId found in chat message, sending to general topic");
                messagingTemplate.convertAndSend("/topic/chat-updates", chatMessage);
            }

        } catch (Exception e) {
            logger.error("Error handling Chat Message: {}", e.getMessage(), e);
        }
    }

    // Timer updates for spectators are published per-game from orchestration using delayed session.




    // Spectator count is published from controllers with a specific gameId.



    @RabbitListener(queues = RabbitMQConfig.MATCHMAKING_QUEUE, containerFactory = "messageListenerContainer")
    public void handleMatchmakingUpdate(Object matchmakingData) {
        try {
            logger.info("Received matchmaking Update: {}", matchmakingData);
            messagingTemplate.convertAndSend("/topic/matchmaking-Updates", matchmakingData);
        } catch (Exception e) {
            logger.error("Error handling matchmaking update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PLAYER_MOVES, containerFactory = "messageListenerContainer")
    public void handleMoveUpdate(Object moveData) {
        try {
            logger.info("Received Move Update: {}", moveData);
            messagingTemplate.convertAndSend("/topic/Player-Moves", moveData);
        } catch (Exception e) {
            logger.error("Error handling move update: {}", e.getMessage(), e);
        }
    }
}