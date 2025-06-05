package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RPGBossRepository extends MongoRepository<RPGBoss, String> {
    List<RPGBoss> findByHpGreaterThan(int minHp);

    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<RPGBoss> findByNameContainingIgnoreCase(String name);
}