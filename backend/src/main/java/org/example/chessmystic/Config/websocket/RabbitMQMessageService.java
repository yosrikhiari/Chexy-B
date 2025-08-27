package org.example.chessmystic.Config.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQMessageService {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendGameUpdate(String gameId, Object gameUpdate){
        try{
            String routingKey = RabbitMQConfig.GAME_UPDATE_RK + "." + gameId;

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHESS_EXCHANGE,
                    routingKey,
                    gameUpdate);
            logger.debug("Game update sent for game: {}", gameId);
        }
        catch(Exception e){
            logger.error(gameId,"   ",e.getMessage());
        }
    }


    public void sendPlayerMoves(String gameId,String playerId, Object move){
        try{
            String routingKey = RabbitMQConfig.PLAYER_MOVES_RK + "." + gameId + "." + playerId;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHESS_EXCHANGE,
                    routingKey,
                    move);
            logger.debug("Player move sent for game: {}, player: {}", gameId, playerId);
        } catch (Exception e) {
            logger.error(gameId,"   ",e.getMessage());
        }
    }

    public void sendTimerUpdate(String gameId, Object timerUpdate){
        try{
            String routingKey = RabbitMQConfig.TIMER_UPDATE_RK + "." + gameId;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHESS_EXCHANGE,
                    routingKey,
                    timerUpdate);
            logger.debug("Timer update sent for game: {}", gameId);
        }
        catch (Exception e) {
            logger.error(gameId,"   ",e.getMessage());
        }
    }

    public void sendMatchmakingUpdate(String playerId, Object matchmakingUpdate){
        try{
            String routingKey = RabbitMQConfig.MATCHMAKING_RK + "." + playerId;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHESS_EXCHANGE,
                    routingKey,
                    matchmakingUpdate);
            logger.debug("Matchmaking update sent for player: {}", playerId);
        }
        catch (Exception e) {
            logger.error(playerId,"   ",e.getMessage());
        }
    }
}
