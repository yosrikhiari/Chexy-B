package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Tounaments.Tournament;

public interface ITournamentService {
    Tournament createTournament(Tournament dto);
    void generateBracket(String tournamentId);
    void scheduleMatches(String tournamentId);

}


