package org.example.chessmystic.Models.UIUX;

import lombok.*;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TieResolutionOption {
    @Id
    private String id;
    private String name;
    private String flavor;
    private String description;
    private int weight;
    private String range;
    private String icon;
    private Rarity rarity;
}