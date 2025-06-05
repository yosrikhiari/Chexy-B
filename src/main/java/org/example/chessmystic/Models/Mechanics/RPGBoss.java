package org.example.chessmystic.Models.Mechanics;

import lombok.*;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.springframework.data.annotation.Id;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RPGBoss {
    @Id
    private String id;

    private String name;
    private String description;
    private int hp;
    private int maxHp;
    private String specialAbility;
    private List<RPGPiece> pieces;
    private String gimmick;
}