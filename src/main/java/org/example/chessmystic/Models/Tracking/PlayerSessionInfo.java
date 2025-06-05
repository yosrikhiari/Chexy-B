package org.example.chessmystic.Models.Tracking;

import lombok.*;
import org.example.chessmystic.Models.Stats.PlayerStats;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSessionInfo {
    @Id
    private String id;

    private String userId; // Link to User.id
    private String keycloakId; // For authentication
    private String username;
    private String displayName; // firstName + lastName
    private String sessionId; // WebSocket/HTTP session
    private boolean isConnected;
    private LocalDateTime lastSeen;
    private PlayerStats currentStats;
}