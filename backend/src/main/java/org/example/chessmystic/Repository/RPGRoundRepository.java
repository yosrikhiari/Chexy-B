package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.rpg.GameObjective;
import org.example.chessmystic.Models.Mechanics.RPGRound;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RPGRoundRepository extends MongoRepository<RPGRound, String> {
    Optional<RPGRound> findByRoundNumber(String roundNumber);
    List<RPGRound> findByObjective(GameObjective objective);
    List<RPGRound> findByBoardSizeGreaterThan(int minSize);

    @Query("{'coinsReward': {$gte: ?0}}")
    List<RPGRound> findHighRewardRounds(int minReward);
}