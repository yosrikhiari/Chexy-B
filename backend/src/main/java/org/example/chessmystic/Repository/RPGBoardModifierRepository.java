package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.rpg.BoardModifierEffect;
import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Models.Transactions.RPGBoardModifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RPGBoardModifierRepository extends MongoRepository<RPGBoardModifier, String> {
    List<RPGBoardModifier> findByEffect(BoardModifierEffect effect);
    List<RPGBoardModifier> findByRarity(Rarity rarity);
    List<RPGBoardModifier> findByIsActiveTrue();
}