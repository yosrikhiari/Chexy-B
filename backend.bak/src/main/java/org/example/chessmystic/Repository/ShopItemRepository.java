package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Transactions.ShopItem;
import org.example.chessmystic.Models.rpg.Rarity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShopItemRepository extends MongoRepository<ShopItem, String> {
    List<ShopItem> findByType(String type);
    List<ShopItem> findByRarity(Rarity rarity);
    List<ShopItem> findByCostLessThanEqual(int maxCost);

    @Query("{'cost': {$gte: ?0, $lte: ?1}}")
    List<ShopItem> findByCostBetween(int minCost, int maxCost);

    @Query("{'type': ?0, 'rarity': ?1}")
    List<ShopItem> findByTypeAndRarity(String type, Rarity rarity);
}