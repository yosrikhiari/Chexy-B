package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.example.chessmystic.Models.rpg.EquipmentSlot;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentItem {
    @Id
    private String id;

    private String name;
    private String description;
    private EquipmentSlot slot;
    private Rarity rarity;

    private int attackDelta;
    private int defenseDelta;
    private int maxHpDelta;

    private Set<String> tags; // e.g. "heroic", "malicious"
}




