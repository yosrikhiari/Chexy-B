package org.example.chessmystic.Models.rpg;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArmyCapacity {
    @Id
    private String id;


    private int maxTotalPieces;
    private int maxQueens;
    private int maxRooks;
    private int maxBishops;
    private int maxKnights;
    private int maxPawns;
    private int bonusCapacity;
}