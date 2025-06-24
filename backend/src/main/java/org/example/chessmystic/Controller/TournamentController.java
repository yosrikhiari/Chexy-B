package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Tounaments.Tournament;
import org.example.chessmystic.Service.interfaces.GameRelated.ITournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tournament")
public class TournamentController {

    private final ITournamentService tournamentService;

    public TournamentController(ITournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<?> createTournament(@RequestBody Tournament tournament) {
        try {
            Tournament createdTournament = tournamentService.createTournament(tournament);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTournament);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create tournament", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/bracket/{tournamentId}")
    public ResponseEntity<?> generateBracket(@PathVariable String tournamentId) {
        try {
            tournamentService.generateBracket(tournamentId);
            return ResponseEntity.ok(Map.of("message", "Bracket generated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate bracket", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/schedule/{tournamentId}")
    public ResponseEntity<?> scheduleMatches(@PathVariable String tournamentId) {
        try {
            tournamentService.scheduleMatches(tournamentId);
            return ResponseEntity.ok(Map.of("message", "Matches scheduled successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to schedule matches", "message", "An unexpected error occurred"));
        }
    }
}