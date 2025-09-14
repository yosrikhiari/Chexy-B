package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Analytics.GameAnalytics;
import org.example.chessmystic.Models.Analytics.UserAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsRepository extends MongoRepository<GameAnalytics, String> {
    
    // Game Analytics queries
    List<GameAnalytics> findByWhitePlayerIdOrBlackPlayerId(String whitePlayerId, String blackPlayerId);
    
    @Query("{ 'gameStartTime': { $gte: ?0, $lte: ?1 } }")
    List<GameAnalytics> findGamesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'gameMode': ?0 }")
    List<GameAnalytics> findGamesByMode(String gameMode);
    
    @Query("{ 'winnerId': ?0 }")
    List<GameAnalytics> findGamesWonByPlayer(String playerId);
    
    @Query("{ '$or': [ { 'whitePlayerId': ?0 }, { 'blackPlayerId': ?0 } ] }")
    List<GameAnalytics> findGamesByPlayer(String playerId);
    
    @Query("{ 'detectedOpening': ?0 }")
    List<GameAnalytics> findGamesByOpening(String opening);
    
    long countByWhitePlayerIdOrBlackPlayerId(String whitePlayerId, String blackPlayerId);
    long countByWinnerId(String winnerId);
    long countByGameMode(String gameMode);
}

