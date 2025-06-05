package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "shop_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItem {
    @Id
    private String id;
    private String name;
    private String description;
    private int cost;
    private String type; // 'piece' | 'modifier'
    private Rarity rarity;
    private Object item; // RPGPiece or RPGModifier
    private List<String> ownedPieceIds;
    private List<String> purchasedModifierIds;
}