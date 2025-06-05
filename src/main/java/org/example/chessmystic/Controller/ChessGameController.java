package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.chess.BoardPosition;
import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Service.interfaces.GameRelated.IChessGameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chess")
public class ChessGameController {

    private final IChessGameService chessGameService;

    public ChessGameController(IChessGameService chessGameService) {
        this.chessGameService = chessGameService;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/validate-move/{gameId}")
    public ResponseEntity<?> validateMove(@PathVariable String gameId, @RequestBody BoardPosition move) {
        try {
            boolean isValid = chessGameService.validateMove(gameId, move);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to validate move", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/execute-move/{gameId}")
    public ResponseEntity<?> executeMove(@PathVariable String gameId, @RequestBody BoardPosition move) {
        try {
            GameState gameState = chessGameService.executeMove(gameId, move);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute move", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/check/{gameId}/{color}")
    public ResponseEntity<?> isCheck(@PathVariable String gameId, @PathVariable PieceColor color) {
        try {
            boolean isCheck = chessGameService.isCheck(gameId, color);
            return ResponseEntity.ok(Map.of("isCheck", isCheck));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check status", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/checkmate/{gameId}/{color}")
    public ResponseEntity<?> isCheckmate(@PathVariable String gameId, @PathVariable PieceColor color) {
        try {
            boolean isCheckmate = chessGameService.isCheckmate(gameId, color);
            return ResponseEntity.ok(Map.of("isCheckmate", isCheckmate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check checkmate", "message", e.getMessage()));
        }
    }
}