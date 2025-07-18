package org.example.chessmystic.Models.Transactions;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "purchase_history")
public class PlayerPurchaseHistory {
    @Id
    private String id;

    private String userId;
    private String gameSessionId;

    private List<ShopItem> shopItems;

    private int costPaid;
    private LocalDateTime purchaseTime;
}
