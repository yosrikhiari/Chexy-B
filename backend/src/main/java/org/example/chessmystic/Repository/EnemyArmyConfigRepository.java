package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.AISystem.EnemyArmyConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnemyArmyConfigRepository extends MongoRepository<EnemyArmyConfig, String> {
    List<EnemyArmyConfig> findByRound(int round);
    List<EnemyArmyConfig> findByDifficulty(int difficulty);
    List<EnemyArmyConfig> findByStrategy(AIStrategy strategy);
    List<EnemyArmyConfig> findByQueenExposedTrue();

    @Query("{'round': ?0, 'difficulty': ?1}")
    Optional<EnemyArmyConfig> findByRoundAndDifficulty(int round, int difficulty);
}