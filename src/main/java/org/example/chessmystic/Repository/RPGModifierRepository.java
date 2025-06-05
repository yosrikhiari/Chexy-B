package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RPGModifierRepository extends MongoRepository<RPGModifier, String> {
    List<RPGModifier> findByRarity(Rarity rarity);
    List<RPGModifier> findByIsActiveTrue();

    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<RPGModifier> findByNameContainingIgnoreCase(String name);
}