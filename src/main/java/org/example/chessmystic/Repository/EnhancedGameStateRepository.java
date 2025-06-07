package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.Mechanics.EnhancedGameState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnhancedGameStateRepository extends MongoRepository<EnhancedGameState, String> {
    List<EnhancedGameState> findByGameSessionId(String gameSessionId);
}