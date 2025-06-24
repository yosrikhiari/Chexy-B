package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Tracking.GameHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameHistoryRepository extends MongoRepository<GameHistory, String> {
    List<GameHistory> findByUserIdsContaining(String userId);
    Optional<GameHistory> findByGameSessionId(String gameSessionId);
    List<GameHistory> findByUserIdsContainingAndIsRankedTrue(String userId);

    List<GameHistory> findByUserIdsContainingAndIsRPGModeTrue(String userId);

    List<GameHistory> findByStartTimeAfter(LocalDateTime since);
}