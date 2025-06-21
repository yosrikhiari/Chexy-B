package org.example.chessmystic.Models.rpg;

import lombok.*;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class
RPGPiece {
    @Id
    private String id;

    private PieceType type;
    private PieceColor color;
    private String name;
    private String description;
    private String specialAbility;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    private Rarity rarity;
    private boolean isJoker = false;
}