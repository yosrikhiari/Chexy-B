package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Service.implementation.GameRelated.ChessGameService;
import org.example.chessmystic.Service.implementation.GameRelated.GameOrchestrationService;
import org.example.chessmystic.Service.implementation.GameRelated.GameSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

    private final GameOrchestrationService gameOrchestrationService;

    public GameController(GameOrchestrationService gameOrchestrationService, ChessGameService chessGameService, GameSessionService gameSessionService) {
        this.gameOrchestrationService = gameOrchestrationService;
    }

    @PostMapping("/games/{gameId}/moves")
    public ResponseEntity<GameState> makeMove(@PathVariable String gameId, @RequestBody BoardPosition move) {
        try {
            GameState gameState = gameOrchestrationService.executeMove(gameId, move);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}