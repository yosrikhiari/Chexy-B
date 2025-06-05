package org.example.chessmystic.Models.rpg;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArmyCapacity {
    private int maxTotalPieces;
    private int maxQueens;
    private int maxRooks;
    private int maxBishops;
    private int maxKnights;
    private int maxPawns;
    private int bonusCapacity;
}