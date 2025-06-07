package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameStateRepository extends MongoRepository<GameState, String> {
    Optional<RPGGameState> findByGameSessionId(String gameSessionId);
}