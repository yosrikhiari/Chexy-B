package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Tounaments.Tournament;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TournamentRepository extends MongoRepository<Tournament, String> {
    List<Tournament> findByParticipantIdsContaining(String participantId);

    @Query("{'startTime': {$gte: ?0}}")
    List<Tournament> findUpcomingTournaments(LocalDateTime now);

    @Query("{'startTime': {$lte: ?0}}")
    List<Tournament> findStartedTournaments(LocalDateTime now);
}