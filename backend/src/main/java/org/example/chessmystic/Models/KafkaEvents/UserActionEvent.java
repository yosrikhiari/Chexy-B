package org.example.chessmystic.Models.KafkaEvents;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActionEvent {
    private String eventId;
    private String userId;
    private long timestamp;
    private String actionType; // LOGIN, LOGOUT, GAME_JOIN, GAME_LEAVE, etc. (not the ones in middle of game)
    private String sessionId;
    private Map<String, Object> metadata;
}