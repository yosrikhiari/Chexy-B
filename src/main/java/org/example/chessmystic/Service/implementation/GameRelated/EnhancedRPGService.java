package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Interactions.CombatResult;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IEnhancedRPGService;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class EnhancedRPGService implements IEnhancedRPGService {

    private final IPlayerActionService playerActionService;
    private final RPGGameStateRepository rpgGameStateRepository;

    @Autowired
    public EnhancedRPGService(IPlayerActionService playerActionService, RPGGameStateRepository rpgGameStateRepository) {
        this.playerActionService = playerActionService;
        this.rpgGameStateRepository = rpgGameStateRepository;
    }

    @Transactional
    @Override
    public CombatResult resolveCombat(EnhancedRPGPiece attacker, EnhancedRPGPiece defender, String gameId) {
        int damage = Math.max(1, attacker.getAttack() - defender.getDefense());
        defender.setCurrentHp(defender.getCurrentHp() - damage);

        boolean defenderDefeated = defender.getCurrentHp() <= 0;
        Integer counterDamage = null;

        if (!defenderDefeated) {
            counterDamage = Math.max(1, defender.getAttack() - attacker.getAttack());
            attacker.setCurrentHp(attacker.getCurrentHp() - counterDamage);
        }

        // Fetch game state to get context
        var gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        // Record combat action
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.CAPTURE,
                -1, -1, -1, -1, // Coordinates TBD based on actual position tracking
                null, gameState.getGameId(), gameState.getCurrentRound(),
                "Combat:" + attacker.getType() + " vs " + defender.getType(),
                damage, defenderDefeated);

        return CombatResult.builder()
                .attacker(attacker)
                .defender(defender)
                .damage(damage)
                .defenderDefeated(defenderDefeated)
                .attackerCounterDamage(counterDamage)
                .build();
    }
    @Override
    @Transactional
    public void applyBoardEffect(String gameId, BoardEffect effect) {
        var gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (gameState.getBoardEffects() == null) {
            gameState.setBoardEffects(new ArrayList<>());
        }

        gameState.getBoardEffects().add(effect);
        rpgGameStateRepository.save(gameState);

        // Record board effect action
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.NORMAL,
                -1, -1, -1, -1,
                null, gameState.getGameId(), gameState.getCurrentRound(),
                "ApplyBoardEffect:" + effect.getName(), 0, false);
    }

    @Override
    @Transactional
    public void handleBossEncounter(String gameId, RPGBoss boss) {
        var gameState = rpgGameStateRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        gameState.setCurrentObjective("Defeat " + boss.getName());
        rpgGameStateRepository.save(gameState);

        // Record boss encounter action
        playerActionService.recordAction(
                gameState.getGameSessionId(), gameState.getUserId(), ActionType.NORMAL,
                -1, -1, -1, -1,
                null, gameState.getGameId(), gameState.getCurrentRound(),
                "BossEncounter:" + boss.getName(), 0, false);
    }
}