package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends MongoRepository<GameSession, String> {
    List<GameSession> findByPlayerIdsContaining(String playerId);
    List<GameSession> findByStatus(GameStatus status);
    List<GameSession> findByGameMode(GameMode gameMode);
    List<GameSession> findByIsActiveTrue();

    @Query("{'playerIds': {$in: [?0]}, 'status': {$in: [?1]}}")
    List<GameSession> findActiveGamesByPlayerId(String playerId, List<GameStatus> activeStatuses);

    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<GameSession> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<GameSession> findByInviteCode(String inviteCode);

    @Query("{ 'enhancedGameStateId': ?0, 'playerIds': { $in: [?1] } }")
    GameSession findByEnhancedGameStateIdAndPlayerId(String enhancedGameStateId, String playerId);

    GameSession findByEnhancedGameStateId(String enhancedGameStateId);
}
