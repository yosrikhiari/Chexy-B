package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.rpg.BoardModifierEffect;
import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Models.Transactions.DynamicBoardModifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicBoardModifierRepository extends MongoRepository<DynamicBoardModifier, String> {
    List<DynamicBoardModifier> findByEffect(BoardModifierEffect effect);
    List<DynamicBoardModifier> findByRarity(Rarity rarity);
    List<DynamicBoardModifier> findByIsActiveTrue();

    @Query("{'minBoardSize': {$lte: ?0}, 'maxBoardSize': {$gte: ?0}}")
    List<DynamicBoardModifier> findCompatibleWithBoardSize(int boardSize);
}