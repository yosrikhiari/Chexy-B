package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapacityModifier {
    @Id
    private String id;
    private String name;
    private String description;
    private String type;
    private PieceType pieceType;
    private int capacityBonus;
    private Rarity rarity;
    private boolean isActive;
}