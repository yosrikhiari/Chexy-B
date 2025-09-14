package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Analytics.UserAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnalyticsRepository extends MongoRepository<UserAnalytics, String> {
    
    Optional<UserAnalytics> findByUserId(String userId);
    
    @Query("{ 'winRate': { $gte: ?0 } }")
    List<UserAnalytics> findUsersWithWinRateAbove(double winRate);
    
    @Query("{ 'totalGamesPlayed': { $gte: ?0 } }")
    List<UserAnalytics> findActiveUsers(int minGames);
    
    @Query("{ 'lastActivityTime': { $gte: ?0 } }")
    List<UserAnalytics> findRecentlyActiveUsers(LocalDateTime since);
    
    @Query("{ 'mostPlayedGameMode': ?0 }")
    List<UserAnalytics> findUsersByPreferredGameMode(String gameMode);
}
