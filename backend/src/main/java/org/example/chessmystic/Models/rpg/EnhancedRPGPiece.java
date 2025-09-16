package org.example.chessmystic.Models.rpg;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedRPGPiece extends RPGPiece {


    private String enhancedName;
    private int currentHp;
    private SpecializationType specialization = SpecializationType.NONE;
    private java.util.Set<WeaknessType> weaknesses = new java.util.HashSet<>();
    private java.util.Set<String> tags = new java.util.HashSet<>();

    @Min(1)
    @Max(100)
    private int level;

    @Min(0)
    private int experience;

    // Constructor that properly calls super
    public EnhancedRPGPiece(RPGPiece basePiece, int currentHp, int level, int experience) {
        super(basePiece.getId(), basePiece.getType(), basePiece.getColor(),
                basePiece.getName(), basePiece.getDescription(), basePiece.getSpecialAbility(),
                basePiece.getHp(), basePiece.getMaxHp(), basePiece.getAttack(),
                basePiece.getDefense(), basePiece.getRarity(), basePiece.isJoker());
        this.currentHp = currentHp;
        this.level = level;
        this.experience = experience;
    }
}