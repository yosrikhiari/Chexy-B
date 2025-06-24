package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Stats.PlayerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends MongoRepository<PlayerProfile, String> {
    Optional<PlayerProfile> findByUserId(String userId);

    @Query("{'highestScore': {$gte: ?0}}")
    List<PlayerProfile> findByHighestScoreGreaterThanEqual(int score);

    @Query(value = "{}", sort = "{'highestScore': -1}")
    List<PlayerProfile> findAllOrderByHighestScoreDesc();

    List<PlayerProfile> findByCurrentGameSessionIdIsNotNull();
}