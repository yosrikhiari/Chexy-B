package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.chess.BoardPosition;

import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Service.interfaces.GameRelated.IChessGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/chess")
public class ChessGameController {

    private final IChessGameService chessGameService;

    @Autowired
    public ChessGameController(IChessGameService chessGameService) {
        this.chessGameService = chessGameService;
    }

    @PostMapping("/{gameId}/validate-move")
    public ResponseEntity<Boolean> validateMove(
            @PathVariable String gameId,
            @RequestBody BoardPosition move) {
        boolean isValid = chessGameService.validateMove(gameId, move);
        return ResponseEntity.ok(isValid);
    }


    @GetMapping("/{gameId}/check-status")
    public ResponseEntity<Boolean> isCheck(
            @PathVariable String gameId,
            @RequestParam PieceColor color) {
        boolean isInCheck = chessGameService.isCheck(gameId, color);
        return ResponseEntity.ok(isInCheck);
    }

    @GetMapping("/{gameId}/checkmate-status")
    public ResponseEntity<Boolean> isCheckmate(
            @PathVariable String gameId,
            @RequestParam PieceColor color) {
        boolean isCheckmate = chessGameService.isCheckmate(gameId, color);
        return ResponseEntity.ok(isCheckmate);
    }


}