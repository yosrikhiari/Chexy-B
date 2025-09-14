package org.example.chessmystic.Service.implementation;

import org.springframework.beans.factory.annotation.Value;
import org.example.chessmystic.Models.KafkaEvents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class GameEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(GameEventProducer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.game-events}")
    private String gameEventsTopic;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    @Value("${kafka.topics.performance-metrics}")
    private String performanceMetricsTopic;

    // Publish move events to Kafka
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishMoveEvent(MoveEvent moveEvent) throws JsonProcessingException, InterruptedException {
        try {
            String key = moveEvent.getGameId(); // Partition by gameId
            String value = objectMapper.writeValueAsString(moveEvent);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(gameEventsTopic, key, value);

            // Wait for the result with timeout
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            logger.debug("Move event published successfully: {} to partition {}", 
                moveEvent.getEventId(), result.getRecordMetadata().partition());
                
        } catch (TimeoutException e) {
            logger.error("Timeout publishing move event: {}", moveEvent.getEventId(), e);
            throw new RuntimeException("Kafka send timeout", e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish move event: {}", moveEvent.getEventId(), e.getCause());
            throw new RuntimeException("Kafka send failed", e.getCause());
        } catch (Exception e) {
            logger.error("Error publishing move event: {}", moveEvent.getEventId(), e);
            throw e;
        }
    }

    // Publish game start events
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishGameStartEvent(GameStartEvent gameStartEvent) throws JsonProcessingException, InterruptedException {
        try {
            String key = gameStartEvent.getGameId();
            String value = objectMapper.writeValueAsString(gameStartEvent);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(gameEventsTopic, key, value);
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            logger.debug("Game start event published successfully: {} to partition {}", 
                gameStartEvent.getEventId(), result.getRecordMetadata().partition());
                
        } catch (TimeoutException e) {
            logger.error("Timeout publishing game start event: {}", gameStartEvent.getEventId(), e);
            throw new RuntimeException("Kafka send timeout", e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish game start event: {}", gameStartEvent.getEventId(), e.getCause());
            throw new RuntimeException("Kafka send failed", e.getCause());
        } catch (Exception e) {
            logger.error("Error publishing game start event: {}", gameStartEvent.getEventId(), e);
            throw e;
        }
    }

    // Publish game end events
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishGameEndEvent(GameEndEvent gameEndEvent) throws JsonProcessingException, InterruptedException {
        try {
            String key = gameEndEvent.getGameId();
            String value = objectMapper.writeValueAsString(gameEndEvent);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(gameEventsTopic, key, value);
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            logger.debug("Game end event published successfully: {} to partition {}", 
                gameEndEvent.getEventId(), result.getRecordMetadata().partition());
                
        } catch (TimeoutException e) {
            logger.error("Timeout publishing game end event: {}", gameEndEvent.getEventId(), e);
            throw new RuntimeException("Kafka send timeout", e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish game end event: {}", gameEndEvent.getEventId(), e.getCause());
            throw new RuntimeException("Kafka send failed", e.getCause());
        } catch (Exception e) {
            logger.error("Error publishing game end event: {}", gameEndEvent.getEventId(), e);
            throw e;
        }
    }

    // Publish user actions
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishUserAction(UserActionEvent userAction) throws JsonProcessingException, InterruptedException {
        try {
            String key = userAction.getUserId();
            String value = objectMapper.writeValueAsString(userAction);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(userActionsTopic, key, value);
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            logger.debug("User action published successfully: {} to partition {}", 
                userAction.getEventId(), result.getRecordMetadata().partition());
                
        } catch (TimeoutException e) {
            logger.error("Timeout publishing user action: {}", userAction.getEventId(), e);
            throw new RuntimeException("Kafka send timeout", e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish user action: {}", userAction.getEventId(), e.getCause());
            throw new RuntimeException("Kafka send failed", e.getCause());
        } catch (Exception e) {
            logger.error("Error publishing user action: {}", userAction.getEventId(), e);
            throw e;
        }
    }

    // Publish performance metrics
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishPerformanceMetrics(PerformanceMetricsEvent metrics) throws JsonProcessingException, InterruptedException {
        try {
            String key = metrics.getGameId();
            String value = objectMapper.writeValueAsString(metrics);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(performanceMetricsTopic, key, value);
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
            logger.debug("Performance metrics published successfully: {} to partition {}", 
                metrics.getEventId(), result.getRecordMetadata().partition());
                
        } catch (TimeoutException e) {
            logger.error("Timeout publishing performance metrics: {}", metrics.getEventId(), e);
            throw new RuntimeException("Kafka send timeout", e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish performance metrics: {}", metrics.getEventId(), e.getCause());
            throw new RuntimeException("Kafka send failed", e.getCause());
        } catch (Exception e) {
            logger.error("Error publishing performance metrics: {}", metrics.getEventId(), e);
            throw e;
        }
    }
}