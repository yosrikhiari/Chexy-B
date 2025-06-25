package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerActionRepository extends MongoRepository<PlayerAction, String> {
    List<PlayerAction> findByGameSessionId(String gameSessionId);
    List<PlayerAction> findByPlayerId(String playerId);
    List<PlayerAction> findByGameSessionIdOrderBySequenceNumberAsc(String gameSessionId);

    @Query("{'gameSessionId': ?0, 'roundNumber': ?1}")
    List<PlayerAction> findByGameSessionIdAndRoundNumber(String gameSessionId, int roundNumber);

    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<PlayerAction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // NEW: Additional queries for better filtering
    @Query("{'gameSessionId': ?0, 'actionType': ?1}")
    List<PlayerAction> findByGameSessionIdAndActionType(String gameSessionId, ActionType actionType);

    @Query("{'playerId': ?0, 'actionType': ?1}")
    List<PlayerAction> findByPlayerIdAndActionType(String playerId, ActionType actionType);

    @Query("{'rpgGameStateId': ?0, 'roundNumber': ?1}")
    List<PlayerAction> findByRpgGameStateIdAndRoundNumber(String rpgGameStateId, int roundNumber);

    @Query("{'gameSessionId': ?0, 'sequenceNumber': {$gte: ?1, $lte: ?2}}")
    List<PlayerAction> findByGameSessionIdAndSequenceNumberBetween(String gameSessionId, int startSeq, int endSeq);
}