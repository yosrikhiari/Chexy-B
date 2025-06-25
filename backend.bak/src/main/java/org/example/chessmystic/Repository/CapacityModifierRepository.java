package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Models.Transactions.CapacityModifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapacityModifierRepository extends MongoRepository<CapacityModifier, String> {
    List<CapacityModifier> findByPieceType(PieceType pieceType);
    List<CapacityModifier> findByRarity(Rarity rarity);
    List<CapacityModifier> findByIsActiveTrue();

    @Query("{'capacityBonus': {$gte: ?0}}")
    List<CapacityModifier> findHighBonusModifiers(int minBonus);
}