package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.example.chessmystic.Models.rpg.BoardModifierEffect;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicBoardModifier {
    @Id
    private String id;

    private String name;
    private String description;
    private BoardModifierEffect effect;
    private Rarity rarity;
    private int sizeModifier;
    private Integer minBoardSize;
    private Integer maxBoardSize;
    private boolean isActive;
}