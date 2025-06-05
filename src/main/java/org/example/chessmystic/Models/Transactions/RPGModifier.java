package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RPGModifier {
    @Id
    private String id;

    private String name;
    private String description;
    private String effect;
    private Rarity rarity;
    private boolean isActive;
}