package org.example.chessmystic.Config.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chessmystic.Models.KafkaEvents.*;
import org.example.chessmystic.Service.implementation.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaAnalyticsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAnalyticsConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(topics = "${kafka.topics.game-events}", groupId = "chexy-analytics-grp")
    public void processGameEvent(String message) {
        try {
            // Parse the event type first
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String eventType = (String) eventMap.get("eventType");

            switch (eventType) {
                case "MOVE":
                    analyticsService.processMoveEvent(objectMapper.readValue(message, MoveEvent.class));
                    break;
                case "GAME_START":
                    analyticsService.processGameStartEvent(objectMapper.readValue(message, GameStartEvent.class));
                    break;
                case "GAME_END":
                    analyticsService.processGameEndEvent(objectMapper.readValue(message, GameEndEvent.class));
                    break;
                default:
                    logger.warn("Unknown game event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing game event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "chexy-analytics-grp")
    public void processUserAction(String message) {
        try {
            UserActionEvent event = objectMapper.readValue(message, UserActionEvent.class);
            analyticsService.processUserAction(event);
        } catch (Exception e) {
            logger.error("Error processing user action: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.performance-metrics}", groupId = "chexy-analytics-grp")
    public void processPerformanceMetrics(String message) {
        try {
            PerformanceMetricsEvent event = objectMapper.readValue(message, PerformanceMetricsEvent.class);
            analyticsService.processPerformanceMetrics(event);
        } catch (Exception e) {
            logger.error("Error processing performance metrics: {}", e.getMessage(), e);
        }
    }

}