package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Stats.PlayerProfile;
import org.example.chessmystic.Models.Tracking.PlayerSessionInfo;
import org.example.chessmystic.Models.UserManagement.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerSessionInfoRepository extends MongoRepository<PlayerSessionInfo, String> {
    Optional<PlayerSessionInfo> findByUserId(final String userId);

    PlayerSessionInfoRepository findBySessionId(String gameId);

    PlayerSessionInfo findBySessionIdAndUserId(String gameId, String playerId);
}