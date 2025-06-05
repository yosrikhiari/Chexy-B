package org.example.chessmystic.Models.AISystem;

import lombok.*;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.springframework.data.annotation.Id;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnemyArmyConfig {
    @Id
    private String id;

    private int round;

    private int difficulty;

    private List<EnhancedRPGPiece> pieces;

    private AIStrategy strategy;

    private boolean queenExposed;
}