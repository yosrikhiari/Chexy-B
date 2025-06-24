package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.Stats.PlayerGameStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerGameStatsRepository extends MongoRepository<PlayerGameStats, String> {

}