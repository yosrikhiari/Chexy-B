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
public class RPGBoardModifier {
    @Id
    private String id;

    private String name;
    private String description;
    private BoardModifierEffect effect;
    private Rarity rarity;
    private Integer boardSizeModifier;
    private Integer specialTiles;
    private boolean isActive;
}