package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Mechanics.BoardConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardConfigurationRepository extends MongoRepository<BoardConfiguration, String> {
    List<BoardConfiguration> findByRound(int round);
    List<BoardConfiguration> findByBossRoundTrue();
    List<BoardConfiguration> findByBoardSizeGreaterThan(int size);
    Optional<BoardConfiguration> findByRoundAndBoardSize(int round, int boardSize);
}
