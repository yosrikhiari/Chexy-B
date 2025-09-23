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

    // Narrative and AI metadata
    private String aiName;
    private String loreSnippet;

    // Special mechanics (opt-in per piece design)
    private Integer oncePerRunControlRemaining; // For Preacher-like pieces
    private DreamerState dreamerState; // For Dreamer pawn
    private String conversationPrompt; // Last prompt that influenced Dreamer

    @Min(1)
    @Max(100)
    private int level;

    @Min(0)
    private int experience;

    private java.util.Set<AbilityId> abilities = new java.util.HashSet<>();
    private java.util.Map<AbilityId, Integer> cooldowns = new java.util.HashMap<>();

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