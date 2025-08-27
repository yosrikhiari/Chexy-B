package org.example.chessmystic.Config.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChessMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ChessMessageListener.class);

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.GAME_UPDATE_QUEUE, containerFactory = "messageListenerContainer")
    public void handleGameUpdate(Object gameUpdate) {
        try {
            logger.info("Received Game Update: {}", gameUpdate);
            messagingTemplate.convertAndSend("/topic/game-Updates", gameUpdate);
        } catch (Exception e) {
            logger.error("Error handling game update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TIMER_UPDATE, containerFactory = "messageListenerContainer")
    public void handleTimerUpdate(Object timerData) {
        try {
            logger.info("Received Timer Update: {}", timerData);
            messagingTemplate.convertAndSend("/topic/timer-Updates", timerData);
        } catch (Exception e) {
            logger.error("Error handling timer update: {}", e.getMessage(), e);
        }
    }

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