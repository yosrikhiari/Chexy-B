package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.UIUX.DragOffset;
import org.example.chessmystic.Models.UIUX.ViewportSize;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "enhanced_game_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedGameState extends RPGGameState {
    private int difficulty;
    private List<EnhancedRPGPiece> enemyArmy;
    private AIStrategy aiStrategy;
    private int teleportPortalsnumber;
    private RoundProgression roundProgression;

    // For UI positioning
    private DragOffset dragOffset;
    private ViewportSize viewportSize;

    // FIXED: Add enhanced-specific linking
    private String enemyAIConfigId; // Link to EnemyAIRequest
    private List<String> bossEncounterIds; // Links to boss encounters
}