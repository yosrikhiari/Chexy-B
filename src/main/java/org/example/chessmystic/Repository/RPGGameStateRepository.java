package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RPGGameStateRepository extends MongoRepository<RPGGameState, String> {
    Optional<RPGGameState> findByGameSessionId(String gameSessionId);
    List<RPGGameState> findByUserId(String userId);
    List<RPGGameState> findByStatus(GameStatus status);

    @Query("{'userId': ?0, 'isGameOver': false}")
    List<RPGGameState> findActiveGamesByUserId(String userId);

    @Query("{'currentRound': {$gte: ?0}}")
    List<RPGGameState> findByCurrentRoundGreaterThanEqual(int round);

    @Query(value = "{'userId': ?0}", sort = "{'score': -1}")
    List<RPGGameState> findByUserIdOrderByScoreDesc(String userId);
}