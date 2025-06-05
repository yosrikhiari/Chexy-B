package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tounaments.Tournament;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Repository.TournamentRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.ITournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TournamentService implements ITournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameSessionService gameSessionService;

    @Autowired
    public TournamentService(TournamentRepository tournamentRepository,
                             GameSessionService gameSessionService) {
        this.tournamentRepository = tournamentRepository;
        this.gameSessionService = gameSessionService;
    }

    @Override
    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Override
    public void generateBracket(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Simple bracket generation - pair participants sequentially
        List<String> participants = tournament.getParticipantIds();
        for (int i = 0; i < participants.size(); i += 2) {
            if (i + 1 < participants.size()) {
                String player1 = participants.get(i);
                String player2 = participants.get(i + 1);

                // Create game session for each match
                GameSession session = gameSessionService.createGameSession(
                        player1, GameMode.TOURNAMENT, false, null);
                gameSessionService.joinGame(session.getGameId(), player2, null);

                tournament.getGameSessionIds().add(session.getGameId());
            }
        }

        tournamentRepository.save(tournament);
    }

    @Override
    public void scheduleMatches(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // In a real implementation, this would schedule matches with proper timing
        // For now, we'll just start all games immediately
        tournament.getGameSessionIds().forEach(gameSessionService::startGame);
    }
}