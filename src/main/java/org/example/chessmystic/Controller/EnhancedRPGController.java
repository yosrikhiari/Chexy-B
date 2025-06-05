package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Interactions.CombatResult;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Service.interfaces.GameRelated.IEnhancedRPGService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enhanced-rpg")
public class EnhancedRPGController {

    private final IEnhancedRPGService enhancedRPGService;
    private final IPlayerActionService playerActionService;

    public EnhancedRPGController(IEnhancedRPGService enhancedRPGService, IPlayerActionService playerActionService) {
        this.enhancedRPGService = enhancedRPGService;
        this.playerActionService = playerActionService;
    }

    // DTO for combat request
    public static class CombatRequest {
        private EnhancedRPGPiece attacker;
        private EnhancedRPGPiece defender;
        private String gameId;

        // Getters and setters
        public EnhancedRPGPiece getAttacker() {
            return attacker;
        }

        public void setAttacker(EnhancedRPGPiece attacker) {
            this.attacker = attacker;
        }

        public EnhancedRPGPiece getDefender() {
            return defender;
        }

        public void setDefender(EnhancedRPGPiece defender) {
            this.defender = defender;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/combat")
    public ResponseEntity<?> resolveCombat(@RequestBody CombatRequest request) {
        try {
            CombatResult result = enhancedRPGService.resolveCombat(request.getAttacker(), request.getDefender(), request.getGameId());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to resolve combat", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/board-effect/{gameId}")
    public ResponseEntity<?> applyBoardEffect(@PathVariable String gameId, @RequestBody BoardEffect effect) {
        try {
            enhancedRPGService.applyBoardEffect(gameId, effect);
            return ResponseEntity.ok(Map.of("message", "Board effect applied successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to apply board effect", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/boss-encounter/{gameId}")
    public ResponseEntity<?> handleBossEncounter(@PathVariable String gameId, @RequestBody RPGBoss boss) {
        try {
            enhancedRPGService.handleBossEncounter(gameId, boss);
            return ResponseEntity.ok(Map.of("message", "Boss encounter handled successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to handle boss encounter", "message", e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/actions/type/{gameSessionId}/{actionType}")
    public ResponseEntity<?> getActionsByType(@PathVariable String gameSessionId, @PathVariable ActionType actionType) {
        try {
            List<PlayerAction> actions = playerActionService.getActionsByType(gameSessionId, actionType);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve actions by type", "message", e.getMessage()));
        }
    }
}