package org.example.chessmystic.Models.AISystem;

import lombok.*;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "enemy_ai_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnemyAIRequest {
    @Id
    private String id;
    private String gameId;
    private int round;
    private int difficulty;
    private List<PlayerAction> moveHistory;
    private AIStrategy strategy;
}