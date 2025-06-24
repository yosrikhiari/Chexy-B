package org.example.chessmystic.Models.Interactions;

import lombok.*;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombatResult {
    private EnhancedRPGPiece attacker;
    private EnhancedRPGPiece defender;
    private int damage;
    private boolean defenderDefeated;
    private Integer attackerCounterDamage;
}